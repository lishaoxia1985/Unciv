package com.unciv.ui.tilegroups

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.ui.utils.*
import kotlin.math.min

/** Helper class for TileGroup, which was getting too full */
class TileGroupIcons(val tileGroup: TileGroup) {

    var improvementIcon: Actor? = null
    var populationIcon: Image? = null //reuse for acquire icon
    val startingLocationIcons = mutableListOf<Actor>()

    var civilianUnitIcon: UnitGroup? = null
    var militaryUnitIcon: UnitGroup? = null
    var aircraftUnitIcon: IconCircleGroup? = null

    fun update(showResourcesAndImprovements: Boolean, showTileYields: Boolean, tileIsViewable: Boolean, showMilitaryUnit: Boolean, viewingCiv: CivilizationInfo?) {
        updateResourceIcon(showResourcesAndImprovements)
        updateImprovementIcon(showResourcesAndImprovements)
        updateStartingLocationIcon(showResourcesAndImprovements)

        if (viewingCiv != null) updateYieldIcon(showTileYields, viewingCiv)
        updateUnitIcon(tileIsViewable, showMilitaryUnit, viewingCiv)
    }

    fun addPopulationIcon(icon: Image = ImageGetter.getStatIcon("Population")
            .apply { color = Color.GREEN.cpy().lerp(Color.BLACK, 0.5f) }) {
        populationIcon?.remove()
        populationIcon = icon
        populationIcon!!.run {
            setSize(20f, 20f)
            center(tileGroup)
            x += 20 // right
        }
        tileGroup.miscLayerGroup.addActor(populationIcon)
    }

    fun removePopulationIcon() {
        populationIcon?.remove()
        populationIcon = null
    }

    private fun updateUnitIcon(tileIsViewable: Boolean, showMilitaryUnit: Boolean, viewingCiv: CivilizationInfo?) {
        civilianUnitIcon?.remove()
        militaryUnitIcon?.remove()
        aircraftUnitIcon?.remove()
        if (tileGroup.tileInfo.civilianUnit != null && tileIsViewable) {
            civilianUnitIcon = unitIcon(tileGroup.tileInfo.civilianUnit!!, viewingCiv)
            tileGroup.unitLayerGroup.addActor(civilianUnitIcon)
        }
        if (tileGroup.tileInfo.militaryUnit != null && tileIsViewable && showMilitaryUnit) {
            militaryUnitIcon = unitIcon(tileGroup.tileInfo.militaryUnit!!, viewingCiv)
            tileGroup.unitLayerGroup.addActor(militaryUnitIcon)
        }
        if (tileGroup.tileInfo.airUnits.isNotEmpty()) {
            aircraftUnitIcon = aircraftIcon(viewingCiv)
            tileGroup.unitLayerGroup.addActor(aircraftUnitIcon)
        }
    }

    private fun unitIcon(unit: MapUnit, viewingCiv: CivilizationInfo?): UnitGroup {
        return UnitGroup(unit, 25f).apply {
            if (unit.baseUnit.isMilitary()) setPosition(15f,35f)
            else setPosition(15f,-5f)
            if (unit.baseUnit.isMilitary() && tileGroup.tileInfo.airUnits.isNotEmpty()) x -= 15
            if (!unit.isIdle() && unit.civInfo == viewingCiv) unitBaseImage.color.a = 0.5f
        }
    }

    private fun aircraftIcon(viewingCiv: CivilizationInfo?): IconCircleGroup {
        val aircraftTable = Table()
        val militaryUnit = tileGroup.tileInfo.militaryUnit
        val ownedNation = if (tileGroup.tileInfo.isCityCenter()) tileGroup.tileInfo.getOwner()!!.nation else militaryUnit!!.civInfo.nation
        if (tileGroup.tileInfo.isCityCenter()) {
            aircraftTable.add(ImageGetter.getImage("OtherIcons/Aircraft")
                    .apply { color = ownedNation.getInnerColor() }).size(12f).row()
            aircraftTable.add("${tileGroup.tileInfo.airUnits.size}".toLabel(ownedNation.getInnerColor(), 10))
        }
        else { // when the militaryUnit is not in CityCenter it should be an aircraft carrier
            val unitCapacity = 2 + militaryUnit!!.getUniques().count { it.text == "Can carry 1 extra air unit" }
            aircraftTable.add("${tileGroup.tileInfo.airUnits.size}/$unitCapacity".toLabel(ownedNation.getInnerColor(),10))
        }
        if (tileGroup.tileInfo.airUnits.none { it.canAttack() } && ownedNation == viewingCiv?.nation)
            aircraftTable.color.a = 0.5f
        return aircraftTable.surroundWithCircle(25f,false).apply {
            circle.color = ownedNation.getOuterColor()
            setPosition(15f,35f)
            if (militaryUnit!=null) x += 15f
        }
    }

    private fun updateImprovementIcon(showResourcesAndImprovements: Boolean) {
        improvementIcon?.remove()
        improvementIcon = null
        if (tileGroup.tileInfo.improvement == null || !showResourcesAndImprovements) return

        val newImprovementImage = ImageGetter.getImprovementIcon(tileGroup.tileInfo.improvement!!)
        tileGroup.miscLayerGroup.addActor(newImprovementImage)
        newImprovementImage.run {
            setSize(20f, 20f)
            center(tileGroup)
            this.x -= 22 // left
            this.y -= 10 // bottom
            color = Color.WHITE.cpy().apply { a = 0.7f }
        }
        improvementIcon = newImprovementImage
    }

    // JN updating display of tile yields
    private fun updateYieldIcon(showTileYields: Boolean, viewingCiv: CivilizationInfo) {

        // Hiding yield icons (in order to update)
        tileGroup.tileYieldGroup.isVisible = false


        if (showTileYields) {
            // Setting up YieldGroup Icon
            tileGroup.tileYieldGroup.setStats(tileGroup.tileInfo.getTileStats(viewingCiv))
            tileGroup.tileYieldGroup.setOrigin(Align.center)
            tileGroup.tileYieldGroup.setScale(0.7f)
            tileGroup.tileYieldGroup.toFront()
            tileGroup.tileYieldGroup.centerX(tileGroup)
            tileGroup.tileYieldGroup.y = tileGroup.height * 0.25f - tileGroup.tileYieldGroup.height / 2
            tileGroup.tileYieldGroup.isVisible = true

            // Adding YieldGroup to miscLayerGroup
            tileGroup.miscLayerGroup.addActor(tileGroup.tileYieldGroup)
        }
    }


    private fun updateResourceIcon(showResourcesAndImprovements: Boolean) {
        if (tileGroup.resource != tileGroup.tileInfo.resource) {
            tileGroup.resource = tileGroup.tileInfo.resource
            tileGroup.resourceImage?.remove()
            if (tileGroup.resource == null) tileGroup.resourceImage = null
            else {
                val newResourceIcon = ImageGetter.getResourceImage(tileGroup.tileInfo.resource!!, 20f)
                newResourceIcon.center(tileGroup)
                newResourceIcon.x = newResourceIcon.x - 22 // left
                newResourceIcon.y = newResourceIcon.y + 10 // top
                tileGroup.miscLayerGroup.addActor(newResourceIcon)
                tileGroup.resourceImage = newResourceIcon
            }
        }

        if (tileGroup.resourceImage != null) { // This could happen on any turn, since resources need certain techs to reveal them
            val shouldDisplayResource =
                    if (tileGroup.showEntireMap) tileGroup.tileInfo.resource != null
                    else showResourcesAndImprovements
                            && tileGroup.tileInfo.hasViewableResource(UncivGame.Current.worldScreen.viewingCiv)
            tileGroup.resourceImage!!.isVisible = shouldDisplayResource
        }
    }


    private fun updateStartingLocationIcon(showResourcesAndImprovements: Boolean) {
        // these are visible in map editor only, but making that bit available here seems overkill

        startingLocationIcons.forEach { it.remove() }
        startingLocationIcons.clear()
        if (!showResourcesAndImprovements) return
        if (tileGroup.forMapEditorIcon) return  // the editor options for terrain do not bother to fully initialize, so tileInfo.tileMap would be an uninitialized lateinit
        val tileInfo = tileGroup.tileInfo
        if (tileInfo.tileMap.startingLocationsByNation.isEmpty()) return

        // Allow display of up to three nations starting locations on the same tile, rest only as count.
        // Sorted so major get precedence and to make the display deterministic, otherwise you could get
        // different stacking order of the same nations in the same editing session
        val nations = tileInfo.tileMap.startingLocationsByNation.asSequence()
            .filter { tileInfo in it.value }
            .filter { it.key in tileInfo.tileMap.ruleset!!.nations } // Ignore missing nations
            .map { it.key to tileInfo.tileMap.ruleset!!.nations[it.key]!! }
            .sortedWith(compareBy({ it.second.isCityState() }, { it.first }))
            .toList()
        if (nations.isEmpty()) return

        val displayCount = min(nations.size, 3)
        var offsetX = (displayCount - 1) * 4f
        var offsetY = (displayCount - 1) * 2f
        for (nation in nations.take(3).asReversed()) {
            val newNationIcon =
                ImageGetter.getNationIndicator(nation.second, 20f)
            tileGroup.miscLayerGroup.addActor(newNationIcon)
            newNationIcon.run {
                setSize(20f, 20f)
                center(tileGroup)
                moveBy(offsetX, offsetY)
                color = Color.WHITE.cpy().apply { a = 0.6f }
            }
            startingLocationIcons.add(newNationIcon)
            offsetX -= 8f
            offsetY -= 4f
        }

        // Add a Label with the total count for this tile
        if (nations.size > 3) {
            // Tons of locations for this tile - display number in red, behind the top three
            startingLocationIcons.add(nations.size.toString().toLabel(Color.BLACK.cpy().apply { a = 0.7f }, 14).apply {
                tileGroup.miscLayerGroup.addActor(this)
                setOrigin(Align.center)
                center(tileGroup)
                moveBy(14.4f, -9f)
            })
            startingLocationIcons.add(nations.size.toString().toLabel(Color.FIREBRICK, 14).apply {
                tileGroup.miscLayerGroup.addActor(this)
                setOrigin(Align.center)
                center(tileGroup)
                moveBy(14f, -8.4f)
            })
        }
    }
}
