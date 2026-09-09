package dev.openfit.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dev.openfit.app.domain.E1RmPoint
import dev.openfit.app.domain.WeightUnit
import dev.openfit.app.domain.UnitConverter

@Composable
fun E1RmChart(points: List<E1RmPoint>, unit: WeightUnit, modifier: Modifier = Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(points, unit) {
        if (points.isNotEmpty()) {
            val x = points.indices.map { (it + 1).toDouble() }
            val y = points.map { UnitConverter.fromKg(it.valueKg, unit).coerceAtLeast(0.0) }
            modelProducer.runTransaction {
                lineSeries { series(x = x, y = y) }
            }
        }
    }

    ProvideVicoTheme(
        rememberM3VicoTheme(
            lineCartesianLayerColors = listOf(MaterialTheme.colorScheme.primary),
            lineColor = MaterialTheme.colorScheme.outlineVariant,
            textColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        val chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom()
        )
        Column(modifier = modifier) {
            if (points.size == 1) {
                Text(
                    "${UnitConverter.displayWeight(points.first().valueKg, unit)} ${unit.label}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your starting point. Log another session to see a trend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (points.isNotEmpty()) {
                val summary = "Estimated one-rep max in ${unit.label}, by session. " +
                    points.mapIndexed { index, point ->
                        "Session ${index + 1}: ${UnitConverter.displayWeight(point.valueKg, unit)} ${unit.label}"
                    }.joinToString(". ")
                CartesianChartHost(
                    chart = chart,
                    modelProducer = modelProducer,
                    modifier = Modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = summary }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${points.size} session${if (points.size == 1) "" else "s"} · e1RM (${unit.label})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            if (points.size > 1) {
                Text(
                    text = "Horizontal axis: session number, not calendar time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
