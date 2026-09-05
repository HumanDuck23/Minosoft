/*
 * Minosoft
 * Copyright (C) 2020-2026 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package dev.spaghett.neatsumo.client

import dev.spaghett.neat.network.Network
import dev.spaghett.neat.network.NetworkData
import dev.spaghett.neatsumo.client.input.InputController
import dev.spaghett.neatsumo.client.input.InputExtractor

class BotController(
    networkData: NetworkData,
    private val inputExtractor: InputExtractor,
    private val inputController: InputController,
) {

    private val maxYawPerTick = 30.0
    private val maxPitchPerTick = 12.0

    private val network = Network(networkData)

    private val outputMapping = mapOf(
        0 to Pair(inputController::startForward, inputController::stopForward),
        1 to Pair(inputController::startBack, inputController::stopBack),
        2 to Pair(inputController::startLeft, inputController::stopLeft),
        3 to Pair(inputController::startRight, inputController::stopRight),
        4 to Pair(inputController::startSneak, inputController::stopSneak),
        5 to Pair(inputController::startSprint, inputController::stopSprint),
        6 to Pair(inputController::startJump, inputController::stopJump),
        7 to Pair(inputController::startAttack, inputController::stopAttack),
    )

    fun tick() {
        try {
            val observation = inputExtractor.extractInfo()

            val inputs = listOf(
                observation.myRadialDist,
                observation.opponentRadialDist,
                observation.sinRelativeAngle,
                observation.cosRelativeAngle,

                observation.myRadialVelo,
                observation.myTangentialVelo,
                observation.opponentRadialVelo,
                observation.opponentTangentialVelo,

                observation.myYawError,
                observation.myPitchError,

                observation.opponentYawError,
                observation.opponentPitchError
            )

            val outputs = network.activate(inputs)
            applyOutputs(outputs)
        } catch (e: InputExtractor.OpponentNotFoundException) {
//            e.printStackTrace()
        } catch (e: InputExtractor.NoAABBException) {
            // e.printStackTrace()
        }

        inputController.tick()
    }

    fun applyOutputs(outputs: List<Double>) {
        check(outputs.size == 12) {
            "Expected 12 outputs from neural net, got ${outputs.size}"
        }

        for ((idx, pair) in outputMapping) {
            val (start, stop) = pair

            if (outputs[idx] >= 0.5) {
                start()
            } else {
                stop()
            }
        }

        val yaw =
            outputs[9] - outputs[8]

        val pitch =
            outputs[11] - outputs[10]

        inputController.addYaw(
            (yaw * maxYawPerTick).toFloat()
        )

        inputController.addPitch(
            (pitch * maxPitchPerTick).toFloat()
        )
    }

    fun stop() {
        inputController.stopAll()
    }

}