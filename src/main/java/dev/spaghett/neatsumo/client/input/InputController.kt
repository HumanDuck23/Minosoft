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

package dev.spaghett.neatsumo.client.input

import de.bixilon.minosoft.data.entities.entities.player.local.LocalPlayerEntity
import de.bixilon.minosoft.protocol.network.session.play.PlaySession

class InputController(
    private val session: PlaySession
) {

    private val player = session.player
    private var attacking = false
    private var ticksSinceAttack = 0

    fun startForward() {
        player.botInput = player.botInput.copy(forward = true)
    }

    fun startBack() {
        player.botInput = player.botInput.copy(backward = true)
    }

    fun startLeft() {
        player.botInput = player.botInput.copy(left = true)
    }

    fun startRight() {
        player.botInput = player.botInput.copy(right = true)
    }

    fun startSneak() {
        player.botInput = player.botInput.copy(sneak = true)
    }

    fun startSprint() {
        player.botInput = player.botInput.copy(sprint = true)
    }

    fun startJump() {
        player.botInput = player.botInput.copy(jump = true)
    }

    fun startAttack() {
        if (!attacking) {
            attacking = true
            ticksSinceAttack = 0
            performAttack()
        }
    }

    fun stopForward() {
        player.botInput = player.botInput.copy(forward = false)
    }

    fun stopBack() {
        player.botInput = player.botInput.copy(backward = false)
    }

    fun stopLeft() {
        player.botInput = player.botInput.copy(left = false)
    }

    fun stopRight() {
        player.botInput = player.botInput.copy(right = false)
    }

    fun stopSneak() {
        player.botInput = player.botInput.copy(sneak = false)
    }

    fun stopSprint() {
        player.botInput = player.botInput.copy(sprint = false)
    }

    fun stopJump() {
        player.botInput = player.botInput.copy(jump = false)
    }

    fun stopAttack() {
        attacking = false
        ticksSinceAttack = 0
    }

    fun addYaw(d: Float) {
        val rotation = player.physics.rotation
        player.physics.forceSetRotation(rotation.copy(yaw = rotation.yaw + d))
    }

    fun addPitch(d: Float) {
        val rotation = player.physics.rotation
        player.physics.forceSetRotation(rotation.copy(pitch = (rotation.pitch + d).coerceIn(-90f, 90f)))
    }

    fun stopAll() {
        stopForward()
        stopBack()
        stopLeft()
        stopRight()
        stopSneak()
        stopSprint()
        stopJump()
        stopAttack()
        syncInput()
    }

    fun tick() {
        syncInput()
        session.camera.interactions.attack.tick()

        if (!attacking) {
            ticksSinceAttack = 0
            return
        }

        ticksSinceAttack++

        val attackIntervalTicks = 2

        if (ticksSinceAttack >= attackIntervalTicks) {
            ticksSinceAttack = 0
            performAttack()
        }
    }

    private fun performAttack() {
        val cam = session.camera
        cam.target.update()
        cam.interactions.tryAttack(true)
        cam.interactions.tryAttack(false)
    }

    private fun syncInput() {
        val bot = player.botInput
        player.input = player.input.copy(
            forward = bot.forward,
            backward = bot.backward,
            left = bot.left,
            right = bot.right,
            jump = bot.jump,
            sneak = bot.sneak,
            sprint = bot.sprint,
        )
    }
}