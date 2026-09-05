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

import de.bixilon.kmath.vec.vec3.d.Vec3d
import de.bixilon.minosoft.data.entities.entities.Entity
import de.bixilon.minosoft.data.registries.shapes.aabb.AABB
import de.bixilon.minosoft.protocol.network.session.play.PlaySession
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class InputExtractor(
    private val session: PlaySession,
    private val arenaCenter: Vec3d,
    private val opponentUUID: UUID,
) {

    data class Observation(
        val myRadialDist: Double,
        val opponentRadialDist: Double,
        val sinRelativeAngle: Double,
        val cosRelativeAngle: Double,

        val myRadialVelo: Double,
        val myTangentialVelo: Double,
        val opponentRadialVelo: Double,
        val opponentTangentialVelo: Double,

        val myYawError: Double,
        val myPitchError: Double,

        val opponentYawError: Double,
        val opponentPitchError: Double,
    )

    class OpponentNotFoundException(uuid: UUID) : IllegalStateException("Opponent with UUID $uuid was not found!")
    class NoAABBException(uuid: UUID) : IllegalStateException("Player with UUID $uuid has no bounding box!")

    fun extractInfo(): Observation {
        val world = session.world
        val player = session.player

        val opponent = world.entities[opponentUUID] ?: throw OpponentNotFoundException(opponentUUID)

        val myPos = player.physics.position
        val opponentPos = opponent.physics.position

        val centerToMe = myPos - arenaCenter
        val centerToOpponent = opponentPos - arenaCenter
        val angle = centerToMe.angle(centerToOpponent)

        // Positional
        val myRadialDist = myPos.horizontalDistanceTo(arenaCenter)
        val opponentRadialDist = opponentPos.horizontalDistanceTo(arenaCenter)
        val sinRelativeAngle = sin(angle)
        val cosRelativeAngle = cos(angle)

        // Velocities
        val myVelocity = polarVelocity(player, arenaCenter)
        val opponentVelocity = polarVelocity(opponent, arenaCenter)

        // Yaw and pitch
        val myTarget = closestPointOnHitbox(
            eye = Vec3d(
                x = myPos.x,
                y = myPos.y + player.eyeHeight,
                z = myPos.z,
            ),
            box = opponent.physics.aabb ?: throw NoAABBException(opponentUUID)
        )
        val opponentTarget = closestPointOnHitbox(
            eye = Vec3d(
                x = opponentPos.x,
                y = opponentPos.y + opponent.eyeHeight,
                z = opponentPos.z,
            ),
            box = player.physics.aabb ?: throw NoAABBException(player.uuid)
        )

        val myAimError = aimError(player, myTarget)
        val myYawError = myAimError.yaw / 180.0
        val myPitchError = myAimError.pitch / 90.0

        val opponentAimError = aimError(opponent, opponentTarget)
        val opponentYawError = opponentAimError.yaw / 180.0
        val opponentPitchError = opponentAimError.pitch / 90.0

        return Observation(
            myRadialDist = myRadialDist,
            opponentRadialDist = opponentRadialDist,
            sinRelativeAngle = sinRelativeAngle,
            cosRelativeAngle = cosRelativeAngle,

            myRadialVelo = myVelocity.radial,
            myTangentialVelo = myVelocity.tangential,
            opponentRadialVelo = opponentVelocity.radial,
            opponentTangentialVelo = opponentVelocity.tangential,

            myYawError = myYawError,
            myPitchError = myPitchError,

            opponentYawError = opponentYawError,
            opponentPitchError = opponentPitchError,
        )
    }

    data class PolarVelocity(
        val radial: Double,
        val tangential: Double,
    )

    private fun polarVelocity(
        entity: Entity,
        center: Vec3d,
    ): PolarVelocity {
        val physics = entity.physics
        val dx = physics.position.x - center.x
        val dz = physics.position.z - center.z

        val length = sqrt(dx * dx + dz * dz)

        if (length == 0.0) {
            return PolarVelocity(0.0, 0.0)
        }

        val radialX = dx / length
        val radialZ = dz / length

        val tangentX = -radialZ
        val tangentZ = radialX

        val velo = entity.physics.velocity

        return PolarVelocity(
            radial =
                velo.x * radialX +
                        velo.z * radialZ,

            tangential =
                velo.x * tangentX +
                        velo.z * tangentZ,
        )
    }

    private fun closestPointOnHitbox(
        eye: Vec3d,
        box: AABB,
    ): Vec3d {
        return Vec3d(
            eye.x.coerceIn(box.min.x, box.max.x),
            eye.y.coerceIn(box.min.y, box.max.y),
            eye.z.coerceIn(box.min.z, box.max.z),
        )
    }

    data class AimError(
        val yaw: Double,
        val pitch: Double,
    )

    fun aimError(
        player: Entity,
        target: Vec3d,
    ): AimError {
        val physics = player.physics

        val eye = Vec3d(
            physics.position.x,
            physics.position.y + player.eyeHeight,
            physics.position.z,
        )

        val ideal = target - eye

        val idealYaw = Math.toDegrees(
            atan2(-ideal.x, ideal.z)
        )

        val horizontal = sqrt(
            ideal.x * ideal.x +
                    ideal.z * ideal.z
        )

        val idealPitch = Math.toDegrees(
            -atan2(ideal.y, horizontal)
        )

        val yawError = wrapDegrees(idealYaw - physics.rotation.yaw)
        val pitchError = idealPitch - physics.rotation.pitch

        return AimError(
            yaw = yawError,
            pitch = pitchError,
        )
    }

    private fun wrapDegrees(angle: Double): Double {
        var wrapped = angle % 360.0

        if (wrapped > 180.0) wrapped -= 360.0
        if (wrapped < -180.0) wrapped += 360.0

        return wrapped
    }

}