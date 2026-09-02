package com.tideglass.surf.provider.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class SurfSpot(val id: String, val name: String, val latitude: Double, val longitude: Double, val region: String)

/** Curated global catalogue shared with pipeline/spots.json. */
object SpotCatalog {
    val spots: List<SurfSpot> = listOf(
        SurfSpot("mundaka", "MUNDAKA", 43.4075, -2.6988, "Spain"),
        SurfSpot("zarautz", "ZARAUTZ", 43.2870, -2.1699, "Spain"),
        SurfSpot("somo", "SOMO", 43.4511, -3.7359, "Spain"),
        SurfSpot("pantin", "PANTIN", 43.6428, -8.1073, "Spain"),
        SurfSpot("el-palmar", "EL PALMAR", 36.2328, -6.0694, "Spain"),
        SurfSpot("famara", "FAMARA", 29.1198, -13.5651, "Spain"),
        SurfSpot("peniche", "PENICHE", 39.3653, -9.3771, "Portugal"),
        SurfSpot("carcavelos", "CARCAVELOS", 38.6791, -9.3378, "Portugal"),
        SurfSpot("nazare", "NAZARE", 39.6029, -9.0700, "Portugal"),
        SurfSpot("ericeira", "ERICEIRA", 38.9870, -9.4190, "Portugal"),
        SurfSpot("hossegor", "HOSSEGOR", 43.6646, -1.4433, "France"),
        SurfSpot("la-torche", "LA TORCHE", 47.8369, -4.3522, "France"),
        SurfSpot("bundoran", "BUNDORAN", 54.4777, -8.2962, "Ireland"),
        SurfSpot("thurso-east", "THURSO EAST", 58.5985, -3.5158, "United Kingdom"),
        SurfSpot("anchor-point", "ANCHOR POINT", 30.5447, -9.7261, "Morocco"),
        SurfSpot("taghazout", "TAGHAZOUT", 30.5450, -9.7080, "Morocco"),
        SurfSpot("jeffreys-bay", "JEFFREYS BAY", -34.0506, 24.9289, "South Africa"),
        SurfSpot("muizenberg", "MUIZENBERG", -34.1085, 18.4708, "South Africa"),
        SurfSpot("skeleton-bay", "SKELETON BAY", -22.9368, 14.4897, "Namibia"),
        SurfSpot("waikiki", "WAIKIKI", 21.2766, -157.8270, "United States"),
        SurfSpot("pipeline", "PIPELINE", 21.6651, -158.0520, "United States"),
        SurfSpot("mavericks", "MAVERICKS", 37.4940, -122.5000, "United States"),
        SurfSpot("trestles", "TRESTLES", 33.3825, -117.5888, "United States"),
        SurfSpot("cocoa-beach", "COCOA BEACH", 28.3200, -80.6076, "United States"),
        SurfSpot("tofino", "TOFINO", 49.0828, -125.9136, "Canada"),
        SurfSpot("puerto-escondido", "PUERTO ESCONDIDO", 15.8537, -97.0521, "Mexico"),
        SurfSpot("pavones", "PAVONES", 8.3901, -83.1376, "Costa Rica"),
        SurfSpot("santa-teresa", "SANTA TERESA", 9.6424, -85.1680, "Costa Rica"),
        SurfSpot("san-juan", "SAN JUAN", 18.4670, -66.1230, "Puerto Rico"),
        SurfSpot("bathsheba", "BATHSHEBA", 13.2134, -59.5202, "Barbados"),
        SurfSpot("montanita", "MONTANITA", -1.8278, -80.7533, "Ecuador"),
        SurfSpot("chicama", "CHICAMA", -7.7027, -79.4470, "Peru"),
        SurfSpot("punta-de-lobos", "PUNTA DE LOBOS", -34.4240, -72.0430, "Chile"),
        SurfSpot("florianopolis", "FLORIANOPOLIS", -27.6600, -48.4800, "Brazil"),
        SurfSpot("mar-del-plata", "MAR DEL PLATA", -38.0530, -57.5270, "Argentina"),
        SurfSpot("uluwatu", "ULUWATU", -8.8151, 115.0884, "Indonesia"),
        SurfSpot("g-land", "G-LAND", -8.7286, 114.3630, "Indonesia"),
        SurfSpot("siargao", "CLOUD 9", 9.8120, 126.1664, "Philippines"),
        SurfSpot("shonan", "SHONAN", 35.3096, 139.4748, "Japan"),
        SurfSpot("arugam-bay", "ARUGAM BAY", 6.8404, 81.8363, "Sri Lanka"),
        SurfSpot("maldives-north", "MALDIVES NORTH", 4.3160, 73.5930, "Maldives"),
        SurfSpot("gold-coast", "GOLD COAST", -28.1652, 153.5500, "Australia"),
        SurfSpot("snapper-rocks", "SNAPPER ROCKS", -28.1637, 153.5504, "Australia"),
        SurfSpot("byron-bay", "BYRON BAY", -28.6474, 153.6127, "Australia"),
        SurfSpot("bells-beach", "BELLS BEACH", -38.3690, 144.2810, "Australia"),
        SurfSpot("margaret-river", "MARGARET RIVER", -33.9760, 114.9820, "Australia"),
        SurfSpot("raglan", "RAGLAN", -37.8040, 174.8220, "New Zealand"),
        SurfSpot("piha", "PIHA", -36.9540, 174.4690, "New Zealand"),
    )

    fun nearest(latitude: Double, longitude: Double): SurfSpot =
        spots.minBy { distanceKm(latitude, longitude, it.latitude, it.longitude) }

    fun byId(id: String): SurfSpot? = spots.firstOrNull { it.id == id }

    fun distanceKm(latitudeA: Double, longitudeA: Double, latitudeB: Double, longitudeB: Double): Double {
        val earthRadiusKm = 6371.0
        val deltaLatitude = Math.toRadians(latitudeB - latitudeA)
        val deltaLongitude = Math.toRadians(longitudeB - longitudeA)
        val a = sin(deltaLatitude / 2).pow(2) + cos(Math.toRadians(latitudeA)) *
            cos(Math.toRadians(latitudeB)) * sin(deltaLongitude / 2).pow(2)
        return earthRadiusKm * 2 * asin(sqrt(a))
    }
}
