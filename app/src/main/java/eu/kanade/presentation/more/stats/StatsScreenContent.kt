package eu.kanade.presentation.more.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.stats.components.StatsItem
import eu.kanade.presentation.more.stats.components.StatsOverviewItem
import eu.kanade.presentation.more.stats.data.StatsData
import eu.kanade.presentation.util.toDurationString
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HikariCard
import tachiyomi.presentation.core.components.HikariCardDefaults
import tachiyomi.presentation.core.components.HikariSectionHeader
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun StatsScreenContent(
    state: StatsScreenState.Success,
    paddingValues: PaddingValues,
) {
    LazyColumn(
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        item {
            OverviewSection(state.overview)
        }
        item {
            HeatmapSection(state.heatmap)
        }
        item {
            TitlesStats(state.titles)
        }
        item {
            ChapterStats(state.chapters)
        }
        item {
            TrackerStats(state.trackers)
        }
    }
}

@Composable
private fun OverviewSection(
    data: StatsData.Overview,
) {
    val none = stringResource(MR.strings.none)
    val context = LocalContext.current
    val readDurationString = remember(data.totalReadDuration) {
        data.totalReadDuration
            .toDuration(DurationUnit.MILLISECONDS)
            .toDurationString(context, fallback = none)
    }
    StatsSectionCard(MR.strings.label_overview_section) {
        StatsMetricRow {
            StatsOverviewItem(
                title = data.libraryMangaCount.toString(),
                subtitle = stringResource(MR.strings.in_library),
                icon = Icons.Outlined.CollectionsBookmark,
                modifier = Modifier.weight(1f),
            )
            StatsVerticalDivider()
            StatsOverviewItem(
                title = readDurationString,
                subtitle = stringResource(MR.strings.label_read_duration),
                icon = Icons.Outlined.Schedule,
                modifier = Modifier.weight(1f),
            )
            StatsVerticalDivider()
            StatsOverviewItem(
                title = data.completedMangaCount.toString(),
                subtitle = stringResource(MR.strings.label_completed_titles),
                icon = Icons.Outlined.LocalLibrary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TitlesStats(
    data: StatsData.Titles,
) {
    StatsSectionCard(MR.strings.label_titles_section) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            val globalUpdate = data.globalUpdateItemCount
            val started = data.startedMangaCount
            val local = data.localMangaCount
            val maxVal = maxOf(globalUpdate, started, local, 1).toFloat()

            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
            ) {
                StatsProgressBar(
                    label = stringResource(MR.strings.label_titles_in_global_update),
                    value = globalUpdate,
                    progress = globalUpdate.toFloat() / maxVal,
                    color = MaterialTheme.colorScheme.primary
                )
                StatsProgressBar(
                    label = stringResource(MR.strings.label_started),
                    value = started,
                    progress = started.toFloat() / maxVal,
                    color = MaterialTheme.colorScheme.secondary
                )
                StatsProgressBar(
                    label = stringResource(MR.strings.label_local),
                    value = local,
                    progress = local.toFloat() / maxVal,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatsProgressBar(
    label: String,
    value: Int,
    progress: Float,
    color: Color,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(color)
            )
        }
    }
}

@Composable
private fun ChapterStats(
    data: StatsData.Chapters,
) {
    StatsSectionCard(MR.strings.chapters) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val total = data.totalChapterCount
            val read = data.readChapterCount
            val progress = if (total > 0) read.toFloat() / total.toFloat() else 0f

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp),
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.size(100.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                StatsDetailRow(
                    label = stringResource(MR.strings.label_total_chapters),
                    value = total.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatsDetailRow(
                    label = stringResource(MR.strings.label_read_chapters),
                    value = read.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatsDetailRow(
                    label = stringResource(MR.strings.label_downloaded),
                    value = data.downloadCount.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun StatsDetailRow(
    label: String,
    value: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TrackerStats(
    data: StatsData.Trackers,
) {
    val notApplicable = stringResource(MR.strings.not_applicable)
    val hasTrackers = data.trackedTitleCount > 0 && !data.meanScore.isNaN()
    val meanScoreStr = remember(data.trackedTitleCount, data.meanScore) {
        if (hasTrackers) {
            "%.2f / 10".format(Locale.ENGLISH, data.meanScore)
        } else {
            notApplicable
        }
    }

    StatsSectionCard(MR.strings.label_tracker_section) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.small),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                StatsDetailRow(
                    label = stringResource(MR.strings.label_tracked_titles),
                    value = data.trackedTitleCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                )
                StatsDetailRow(
                    label = stringResource(MR.strings.label_mean_score),
                    value = meanScoreStr,
                    color = if (hasTrackers) Color(0xFFFBC02D) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatsDetailRow(
                    label = stringResource(MR.strings.label_used),
                    value = data.trackerCount.toString(),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            if (hasTrackers) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.label_mean_score),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    RatingStars(
                        rating = data.meanScore.toFloat() / 2f,
                        maxRating = 5,
                        starSize = 20.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingStars(
    rating: Float,
    maxRating: Int = 5,
    starSize: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxRating) {
            val fraction = (rating - (i - 1)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.size(starSize)
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                if (fraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RectangleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(starSize),
                            tint = Color(0xFFFBC02D)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapSection(
    data: StatsData.HistoryHeatmap,
) {
    StatsSectionCard(MR.strings.label_heatmap_section) {
        var selectedDay by remember { mutableStateOf<Pair<Long, Int>?>(null) }
        val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

        val days = remember(data.history) {
            val list = mutableListOf<Pair<Long, Int>>()
            val cal = Calendar.getInstance()

            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.add(Calendar.WEEK_OF_YEAR, -52)

            repeat(53 * 7) {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val time = cal.timeInMillis
                list.add(time to (data.history[time] ?: 0))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            list
        }

        val primaryColor = MaterialTheme.colorScheme.primary
        val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.padding.small),
        ) {
            val scrollState = rememberScrollState(Int.MAX_VALUE)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .padding(horizontal = MaterialTheme.padding.medium),
            ) {
                days.chunked(7).forEach { week ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        week.forEach { (time, count) ->
                            val color = when {
                                count == 0 -> surfaceColor.copy(alpha = 0.15f)
                                count <= 3 -> primaryColor.copy(alpha = 0.3f)
                                count <= 7 -> primaryColor.copy(alpha = 0.5f)
                                count <= 12 -> primaryColor.copy(alpha = 0.8f)
                                else -> primaryColor
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(color)
                                    .clickable {
                                        selectedDay = if (selectedDay?.first == time) null else time to count
                                    },
                            ) {
                                if (selectedDay?.first == time) {
                                    Popup(
                                        alignment = Alignment.TopCenter,
                                        offset = IntOffset(0, -40),
                                        onDismissRequest = { selectedDay = null },
                                        properties = PopupProperties(focusable = false),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant,
                                                    MaterialTheme.shapes.medium,
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = dateFormatter.format(time),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Text(
                                                    text = pluralStringResource(
                                                        MR.plurals.manga_num_chapters,
                                                        count,
                                                        count,
                                                    ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            val legendColors = listOf(
                surfaceColor.copy(alpha = 0.15f),
                primaryColor.copy(alpha = 0.3f),
                primaryColor.copy(alpha = 0.5f),
                primaryColor.copy(alpha = 0.8f),
                primaryColor
            )
            legendColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Spacer(modifier = Modifier.width(1.dp))
            Text(
                text = "More",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsSectionCard(
    titleRes: StringResource,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        HikariSectionHeader(text = stringResource(titleRes))
        HikariCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.padding.medium),
                content = content,
            )
        }
    }
}

@Composable
private fun StatsMetricRow(
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        content = content,
    )
}

@Composable
private fun StatsVerticalDivider() {
    VerticalDivider(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = MaterialTheme.padding.small),
        thickness = 0.5.dp,
        color = HikariCardDefaults.dividerColor(),
    )
}

