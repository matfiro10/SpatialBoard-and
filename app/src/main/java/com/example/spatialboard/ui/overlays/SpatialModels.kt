package com.example.spatialboard.ui.overlays

data class BoardItem(
    val id: String,
    val title: String,
    val roomLabel: String,
    val beaconId: String?,
    val updatedAt: String,
    val content: String,
    val mediaUri: String? = null,
    val tags: List<String> = emptyList()
)

data class BeaconItem(
    val id: String,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val name: String,
    val macAddress: String,
    val zoneLabel: String,
    val distanceMeters: Float,
    val rssiDbm: Int,
    val txPowerDbm: Int,
    val lastSeenLabel: String,
    val linkedBoardsCount: Int,
    val isInRange: Boolean
)

object SampleSpatialData {
    val initialBoards = listOf(
        BoardItem(
            id = "1",
            title = "Harmonogram zajęć",
            roomLabel = "Sala 204 • Laboratorium IoT",
            beaconId = "b1",
            updatedAt = "Dzisiaj, 14:20",
            content = "Poniedziałek 10:00 — Laboratorium systemów wbudowanych i czujników BLE.\nŚroda 12:15 — Konsultacje projektowe przy stanowisku 3.",
            tags = listOf("IoT", "Lab 204")
        ),
        BoardItem(
            id = "2",
            title = "Instrukcja stanowiska",
            roomLabel = "Sala 112 • Robotyka",
            beaconId = "b2",
            updatedAt = "Wczoraj, 18:05",
            content = "1. Sprawdź krańcówki osi X i Y przed podaniem napięcia.\n2. Uruchom kontroler w trybie bezpiecznym.\n3. Po zakończeniu pracy odłącz zasilanie główne.",
            tags = listOf("BHP", "Robotyka")
        ),
        BoardItem(
            id = "3",
            title = "Notatki projektowe",
            roomLabel = "Strefa wspólna • Piętro 1",
            beaconId = null,
            updatedAt = "18 lis, 09:40",
            content = "Dodać lokalną pamięć podręczną Room dla boardów oraz synchronizację kotwic przestrzennych z serwerem po wykryciu beacona.",
            tags = listOf("Architektura", "ARCore")
        )
    )

    val initialBeacons = listOf(
        BeaconItem(
            id = "b1",
            uuid = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
            major = 2,
            minor = 204,
            name = "Beacon-204-A",
            macAddress = "C4:8F:12:9A:01:B2",
            zoneLabel = "Laboratorium IoT",
            distanceMeters = 1.4f,
            rssiDbm = -56,
            txPowerDbm = -59,
            lastSeenLabel = "2 s temu",
            linkedBoardsCount = 2,
            isInRange = true
        ),
        BeaconItem(
            id = "b2",
            uuid = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0",
            major = 1,
            minor = 112,
            name = "Beacon-112-B",
            macAddress = "D8:3A:44:7E:19:04",
            zoneLabel = "Pracownia sieciowa",
            distanceMeters = 4.8f,
            rssiDbm = -74,
            txPowerDbm = -59,
            lastSeenLabel = "5 s temu",
            linkedBoardsCount = 1,
            isInRange = true
        )
    )
}
