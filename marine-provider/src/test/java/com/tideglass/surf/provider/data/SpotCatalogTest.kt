package com.tideglass.surf.provider.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotCatalogTest {
    @Test
    fun selectsNearestKnownSpot() {
        assertEquals("MUNDAKA", SpotCatalog.nearest(43.407, -2.699).name)
        assertEquals("FAMARA", SpotCatalog.nearest(29.12, -13.55).name)
    }
}
