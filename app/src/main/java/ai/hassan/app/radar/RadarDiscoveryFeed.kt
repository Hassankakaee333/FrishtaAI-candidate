package ai.hassan.app.radar

import ai.hassan.app.data.RadarFindingEntity

/** Pluggable radar feed — Radar 2.0 foundation. */
interface RadarDiscoveryFeed {
    val feedId: String
    suspend fun discover(): List<RadarFindingEntity>
}

class CompositeRadarScanner(
    private val feeds: List<RadarDiscoveryFeed>,
) : RadarScanner {
    override suspend fun scan(): List<RadarFindingEntity> =
        feeds.flatMap { feed -> feed.discover() }
}

/** Wraps OfficialSourceRadar as a feed. */
class GitHubReleaseFeed(
    private val scanner: OfficialSourceRadar,
) : RadarDiscoveryFeed {
    override val feedId: String = "github-releases"

    override suspend fun discover(): List<RadarFindingEntity> = scanner.scan()
}
