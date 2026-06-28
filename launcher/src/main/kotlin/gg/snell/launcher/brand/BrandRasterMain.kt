package gg.snell.launcher.brand

import org.apache.batik.transcoder.SVGAbstractTranscoder
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ByteArrayInputStream
import java.io.OutputStream
import javax.imageio.ImageIO

/**
 * Rasterizes a brand SVG to PNGs (and a Windows .ico) with Apache Batik — pure Java, no
 * native libs, so it runs headless. Used by the `rasterizeBrand` Gradle task; not shipped.
 */
fun main(args: Array<String>) {
    val src = File(if (args.isNotEmpty()) args[0] else "docs/brand/snell-icon.svg")
    val outDir = File("launcher/build/brand").apply { mkdirs() }
    val sizes = intArrayOf(16, 20, 24, 32, 40, 48, 64, 128, 256, 512)
    val pngs = LinkedHashMap<Int, ByteArray>()
    for (size in sizes) {
        val t = PNGTranscoder()
        t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, size.toFloat())
        t.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, size.toFloat())
        val bos = ByteArrayOutputStream()
        src.inputStream().use { ins -> t.transcode(TranscoderInput(ins), TranscoderOutput(bos)) }
        val bytes = bos.toByteArray()
        pngs[size] = bytes
        File(outDir, "icon-$size.png").writeBytes(bytes)
        println("wrote icon-$size.png (${bytes.size} bytes)")
    }
    // Hybrid .ico: uncompressed BMP/DIB for the small sizes Windows Explorer actually uses
    // (PNG-only ICO entries render poorly at 16/32/48 in the shell), PNG for 256.
    val icoSizes = listOf(16, 24, 32, 48, 64, 128, 256)
    val icoImages = LinkedHashMap<Int, ByteArray>()
    for (s in icoSizes) {
        icoImages[s] = if (s >= 256) pngs.getValue(s) else dibIcon(ImageIO.read(ByteArrayInputStream(pngs.getValue(s))))
    }
    File(outDir, "snell.ico").writeBytes(packIco(icoImages))
    println("wrote snell.ico (BMP<=128 + PNG256)")

    // Horizontal lockup (non-square): render by width, preserve aspect.
    val logo = File("docs/brand/snell-logo.svg")
    if (logo.exists()) {
        val t = PNGTranscoder()
        t.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, 1080f)
        val bos = ByteArrayOutputStream()
        logo.inputStream().use { ins -> t.transcode(TranscoderInput(ins), TranscoderOutput(bos)) }
        File(outDir, "logo.png").writeBytes(bos.toByteArray())
        println("wrote logo.png (${bos.size()} bytes)")
    }
}

/** 32-bit BGRA DIB (BITMAPINFOHEADER) for a .ico entry — bottom-up XOR bitmap + empty AND mask. */
private fun dibIcon(img: java.awt.image.BufferedImage): ByteArray {
    val w = img.width; val h = img.height
    val o = ByteArrayOutputStream()
    le32(o, 40); le32(o, w); le32(o, h * 2)            // header size; height doubled (XOR + AND)
    le16(o, 1); le16(o, 32); le32(o, 0); le32(o, 0)    // planes, bpp, BI_RGB, sizeImage
    le32(o, 0); le32(o, 0); le32(o, 0); le32(o, 0)     // ppm x/y, colorsUsed/Important
    for (y in h - 1 downTo 0) for (x in 0 until w) {
        val argb = img.getRGB(x, y)
        o.write(argb and 0xFF); o.write((argb shr 8) and 0xFF); o.write((argb shr 16) and 0xFF); o.write((argb shr 24) and 0xFF)
    }
    o.write(ByteArray((((w + 31) / 32) * 4) * h))      // 1bpp AND mask, all opaque (alpha drives transparency)
    return o.toByteArray()
}

/** Packs PNGs into a Windows .ico (PNG-compressed entries; Vista+). */
private fun packIco(images: Map<Int, ByteArray>): ByteArray {
    val n = images.size
    var offset = 6 + n * 16
    val dir = ByteArrayOutputStream()
    val body = ByteArrayOutputStream()
    dir.write(0); dir.write(0); dir.write(1); dir.write(0)      // reserved=0, type=1 (icon)
    le16(dir, n)
    for ((size, png) in images) {
        val dim = if (size >= 256) 0 else size                 // 0 means 256
        dir.write(dim); dir.write(dim); dir.write(0); dir.write(0)
        le16(dir, 1); le16(dir, 32)                            // planes, bpp
        le32(dir, png.size); le32(dir, offset)
        offset += png.size
        body.write(png)
    }
    return ByteArrayOutputStream().apply { write(dir.toByteArray()); write(body.toByteArray()) }.toByteArray()
}

private fun le16(o: OutputStream, v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF) }
private fun le32(o: OutputStream, v: Int) { o.write(v and 0xFF); o.write((v shr 8) and 0xFF); o.write((v shr 16) and 0xFF); o.write((v shr 24) and 0xFF) }
