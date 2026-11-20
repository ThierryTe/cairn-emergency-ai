package com.cairn.emergency.feature.kit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cairn.emergency.R
import com.cairn.emergency.ui.components.JourneyNavy
import com.cairn.emergency.ui.components.JourneyOutline
import com.cairn.emergency.ui.components.JourneyPurple
import com.cairn.emergency.ui.components.JourneyScaffold
import com.cairn.emergency.ui.components.JourneyTeal
import com.cairn.emergency.ui.home.CairnGlyph
import com.cairn.emergency.ui.home.CairnIcon
import java.text.NumberFormat

private val CheckedGreen = Color(0xFF07853D)

@Composable
internal fun kitPercentLabel(percent: Int): String =
    NumberFormat.getPercentInstance(LocalConfiguration.current.locales[0]).format(percent / 100.0)

@Composable
internal fun KitScreen(model: KitViewModel, onHousehold: () -> Unit, onBack: () -> Unit, onCheckFeedback: () -> Unit) {
    var category by rememberSaveable { mutableStateOf<KitCategory?>(null) }
    val plan = model.plan
    val current = category
    val back = if (current == null) onBack else { { category = null } }
    BackHandler(enabled = current != null, onBack = back)

    JourneyScaffold(stringResource(current?.label ?: R.string.kit_title), back) {
        key(current) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                model.issue?.let { issue ->
                    Text(stringResource(issue), Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = JourneyNavy)
                    if (plan == null) {
                        TextButton(onClick = model::observe) { Text(stringResource(R.string.retry)) }
                    }
                }
                if (plan == null) {
                    if (model.issue == null) PersonalizingKit()
                } else if (current == null) {
                    KitHouseholdBanner(plan, onHousehold)
                    KitProgress(plan, Modifier.align(Alignment.CenterHorizontally))
                    KitCategory.entries.forEach { group ->
                        val items = plan.items.filter { it.supply.category == group }
                        CategoryCard(group, items, plan) { category = group }
                    }
                } else {
                    KitHouseholdBanner(plan, onHousehold, categoryPage = true)
                    CategoryChecklist(plan, current, model.pending) { item, checked ->
                        model.setChecked(item, checked)
                        onCheckFeedback()
                    }
                    KitExplanation(plan, current)
                }
            }
        }
    }
}

@Composable
private fun PersonalizingKit() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 42.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CairnGlyph(CairnIcon.Kit, JourneyTeal, Modifier.size(62.dp))
        Text(stringResource(R.string.kit_personalizing), color = JourneyNavy, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = JourneyTeal)
    }
}

@Composable
private fun KitHouseholdBanner(plan: KitPlan, onHousehold: () -> Unit, categoryPage: Boolean = false) {
    Surface(
        onClick = onHousehold,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = if (categoryPage) Color(0xFFDDEAFB) else Color(0xFFE2E5FA),
    ) {
        Row(Modifier.heightIn(min = 56.dp).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            CairnGlyph(CairnIcon.Household, Color(0xFF086BDD), Modifier.size(31.dp))
            Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                Text(
                    stringResource(
                        when {
                            plan.householdCountsIncomplete -> R.string.kit_confirm_household
                            categoryPage -> R.string.kit_recommended_household
                            else -> R.string.kit_personalized_household
                        },
                    ),
                    color = JourneyNavy,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(householdSummary(plan), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
            }
            CairnGlyph(CairnIcon.Next, JourneyNavy, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun householdSummary(plan: KitPlan): String {
    val people = plan.people?.let { pluralStringResource(R.plurals.kit_people, it, it) }
    val pets = plan.petCount?.let { pluralStringResource(R.plurals.kit_pet_count, it, it) }
        ?: stringResource(R.string.kit_pet_count_unknown).takeIf { plan.petSuppliesNeeded }
    return listOfNotNull(
        people,
        pets,
        stringResource(R.string.kit_includes_medical).takeIf { plan.medicalDetails != null },
        stringResource(R.string.kit_includes_access).takeIf { plan.accessibilityDetails != null },
    ).joinToString(" · ").ifBlank { stringResource(R.string.kit_using_baseline) }
}

@Composable
private fun KitProgress(plan: KitPlan, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier.size(133.dp).semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(plan.checkedCount.toFloat(), 0f..plan.items.size.toFloat())
            },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val start = Offset(stroke / 2f, stroke / 2f)
                val ring = Size(size.width - stroke, size.height - stroke)
                drawArc(Color(0xFFD9E1EC), -90f, 360f, false, start, ring, style = Stroke(stroke))
                if (plan.checkedCount > 0) {
                    drawArc(
                        Brush.linearGradient(listOf(Color(0xFF0B697D), JourneyTeal, CheckedGreen)),
                        -90f, 360f * plan.checkedCount / plan.items.size, false, start, ring,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(kitPercentLabel(plan.readyPercent), color = JourneyNavy, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    stringResource(if (plan.householdCountsIncomplete) R.string.kit_checked else R.string.kit_ready),
                    color = JourneyNavy,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            stringResource(R.string.kit_item_progress, plan.checkedCount, plan.items.size),
            color = JourneyNavy,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CategoryCard(category: KitCategory, items: List<KitItem>, plan: KitPlan, onClick: () -> Unit) {
    val (icon, tint) = when (category) {
        KitCategory.WaterFood -> CairnIcon.Water to Color(0xFF0879DE)
        KitCategory.HealthHygiene -> CairnIcon.Health to JourneyPurple
        KitCategory.SafetyTools -> CairnIcon.Tools to JourneyPurple
        KitCategory.DocumentsEssentials -> CairnIcon.Documents to JourneyPurple
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, JourneyOutline),
    ) {
        Row(Modifier.heightIn(min = 60.dp).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            CairnGlyph(icon, tint, Modifier.size(24.dp))
            Column(Modifier.weight(1f).padding(horizontal = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(category.label), color = JourneyNavy, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val note = when (category) {
                    KitCategory.WaterFood -> R.string.kit_category_water_note.takeIf { plan.petSuppliesNeeded }
                    KitCategory.HealthHygiene -> R.string.kit_includes_medical.takeIf { plan.medicalDetails != null }
                    KitCategory.SafetyTools -> R.string.kit_includes_access.takeIf { plan.accessibilityDetails != null }
                    KitCategory.DocumentsEssentials -> null
                }
                if (note != null) Text(stringResource(note), color = JourneyNavy, style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.kit_category_progress, items.count { it.checked }, items.size), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
            }
            CairnGlyph(CairnIcon.Next, JourneyNavy, Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CategoryChecklist(
    plan: KitPlan,
    category: KitCategory,
    pending: Set<KitSupply>,
    onChecked: (KitItem, Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    plan.items.filter { it.supply.category == category }.forEach { item ->
        val label = stringResource(item.supply.label)
        val isExpanded = expanded == item.supply.id
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, JourneyOutline),
        ) {
            Column {
                Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.checked,
                        onCheckedChange = { onChecked(item, it) },
                        enabled = item.supply !in pending,
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = label },
                        colors = CheckboxDefaults.colors(checkedColor = CheckedGreen),
                    )
                    Row(
                        Modifier.weight(1f).heightIn(min = 60.dp)
                            .clickable(role = Role.Button, onClickLabel = stringResource(if (isExpanded) R.string.kit_hide_details else R.string.kit_show_details)) {
                                expanded = if (isExpanded) null else item.supply.id
                            }
                            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(label, color = JourneyNavy, fontWeight = FontWeight.Bold)
                            Text(kitTarget(item.supply, plan), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
                        }
                        CairnGlyph(if (isExpanded) CairnIcon.Next else CairnIcon.Down, JourneyNavy, Modifier.size(20.dp))
                    }
                }
                if (isExpanded) {
                    Column(Modifier.padding(start = 49.dp, end = 12.dp, bottom = 12.dp)) {
                        Text(kitReason(item.supply), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
                        val profileDetail = when (item.supply) {
                            KitSupply.Medications, KitSupply.MedicalDocuments -> plan.medicalDetails
                            KitSupply.AccessibilityAids -> plan.accessibilityDetails
                            else -> null
                        }
                        if (profileDetail != null) {
                            Text(stringResource(R.string.kit_profile_detail, profileDetail), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KitExplanation(plan: KitPlan, category: KitCategory) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF7DE),
        border = BorderStroke(1.dp, Color(0xFFF3DB9C)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(R.string.kit_why_amount), color = JourneyNavy, fontWeight = FontWeight.Bold)
            Text(householdSummary(plan), color = JourneyNavy, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.kit_based_on_household), color = JourneyNavy, style = MaterialTheme.typography.bodySmall)
            if (category == KitCategory.WaterFood) {
                Text(
                    stringResource(R.string.kit_water_reason, KitPlan.WaterLitersPerPerson, KitPlan.Days),
                    color = JourneyNavy,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(stringResource(R.string.kit_source), color = JourneyNavy, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val KitCategory.label: Int
    get() = when (this) {
        KitCategory.WaterFood -> R.string.kit_group_water_food
        KitCategory.HealthHygiene -> R.string.kit_group_health
        KitCategory.SafetyTools -> R.string.kit_group_safety
        KitCategory.DocumentsEssentials -> R.string.kit_group_documents
    }

private val KitSupply.label: Int
    get() = when (this) {
        KitSupply.Water -> R.string.kit_supply_water
        KitSupply.Food -> R.string.kit_supply_food
        KitSupply.PetFood -> R.string.kit_supply_pet_food
        KitSupply.WaterContainer -> R.string.kit_supply_container
        KitSupply.PetWater -> R.string.kit_supply_pet_water
        KitSupply.FirstAid -> R.string.kit_supply_first_aid
        KitSupply.Hygiene -> R.string.kit_supply_hygiene
        KitSupply.Medications -> R.string.kit_supply_medications
        KitSupply.BabySupplies -> R.string.kit_baby
        KitSupply.Flashlight -> R.string.kit_supply_flashlight
        KitSupply.Radio -> R.string.kit_supply_radio
        KitSupply.PowerBank -> R.string.kit_supply_power_bank
        KitSupply.AccessibilityAids -> R.string.kit_supply_access
        KitSupply.EssentialDocuments -> R.string.kit_supply_documents
        KitSupply.Contacts -> R.string.kit_supply_contacts
        KitSupply.MedicalDocuments -> R.string.kit_supply_medical_docs
    }

@Composable
private fun kitTarget(supply: KitSupply, plan: KitPlan): String = when (supply) {
    KitSupply.Water -> plan.waterLiters?.let { stringResource(R.string.kit_target_water_total, it, KitPlan.Days) }
        ?: stringResource(R.string.kit_target_water_person, KitPlan.WaterLitersPerPerson, KitPlan.Days)
    KitSupply.Food -> stringResource(R.string.kit_food_household_target, KitPlan.Days)
    KitSupply.PetFood, KitSupply.PetWater -> plan.petCount?.let { count ->
        stringResource(R.string.kit_pet_day_target, KitPlan.Days, pluralStringResource(R.plurals.kit_pet_count, count, count))
    } ?: stringResource(R.string.kit_pet_day_unspecified, KitPlan.Days)
    KitSupply.WaterContainer -> plan.waterLiters?.let { stringResource(R.string.kit_target_container_liters, it) }
        ?: stringResource(R.string.kit_target_container)
    KitSupply.FirstAid, KitSupply.Flashlight, KitSupply.Radio, KitSupply.PowerBank -> stringResource(R.string.kit_target_one)
    KitSupply.Hygiene -> stringResource(R.string.kit_food_household_target, KitPlan.Days)
    KitSupply.Medications -> stringResource(R.string.kit_target_medications)
    KitSupply.BabySupplies -> stringResource(R.string.kit_baby_target, KitPlan.Days)
    KitSupply.AccessibilityAids -> stringResource(R.string.kit_target_access)
    KitSupply.EssentialDocuments, KitSupply.Contacts, KitSupply.MedicalDocuments -> stringResource(R.string.kit_target_copy)
}

@Composable
private fun kitReason(supply: KitSupply): String = when (supply) {
    KitSupply.Water -> stringResource(R.string.kit_water_reason, KitPlan.WaterLitersPerPerson, KitPlan.Days)
    KitSupply.Food -> stringResource(R.string.kit_food_reason, KitPlan.Days)
    KitSupply.PetFood -> stringResource(R.string.kit_pets_reason, KitPlan.Days)
    KitSupply.WaterContainer -> stringResource(R.string.kit_reason_container)
    KitSupply.PetWater -> stringResource(R.string.kit_reason_pet_water)
    KitSupply.FirstAid -> stringResource(R.string.kit_reason_first_aid)
    KitSupply.Hygiene -> stringResource(R.string.kit_reason_hygiene)
    KitSupply.Medications -> stringResource(R.string.kit_reason_medications)
    KitSupply.BabySupplies -> stringResource(R.string.kit_baby_reason, KitPlan.Days)
    KitSupply.Flashlight -> stringResource(R.string.kit_reason_flashlight)
    KitSupply.Radio -> stringResource(R.string.kit_reason_radio)
    KitSupply.PowerBank -> stringResource(R.string.kit_reason_power_bank)
    KitSupply.AccessibilityAids -> stringResource(R.string.kit_reason_access)
    KitSupply.EssentialDocuments -> stringResource(R.string.kit_reason_documents)
    KitSupply.Contacts -> stringResource(R.string.kit_reason_contacts)
    KitSupply.MedicalDocuments -> stringResource(R.string.kit_reason_medical_docs)
}
