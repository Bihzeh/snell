package gg.snell.launcher.ui

import gg.snell.launcher.ui.components.Brand
import kotlin.test.Test
import kotlin.test.assertNotNull

class BrandAssetsTest {
    /** The brand logos are loaded by path at runtime, so a renamed/missing asset would only
     *  surface as a blank window icon or NPE in the running app. Guard the referenced paths
     *  at build time instead. */
    @Test
    fun referencedBrandAssetsResolveOnClasspath() {
        for (path in listOf(Brand.APP_ICON, Brand.TILE, Brand.WATERMARK)) {
            assertNotNull(
                javaClass.classLoader.getResource(path),
                "missing brand asset on classpath: $path",
            )
        }
    }
}
