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

import de.bixilon.minosoft.util.logging.Log
import de.bixilon.minosoft.util.logging.LogLevels
import de.bixilon.minosoft.util.logging.LogMessageType
import dev.spaghett.neatsumo.protocol.message.LoadNetwork
import dev.spaghett.neatsumo.protocol.message.RegisterClient
import dev.spaghett.neatsumo.protocol.message.StartControl
import dev.spaghett.neatsumo.protocol.message.StopControl
import dev.spaghett.neatsumo.protocol.socket.SocketClient
import java.util.UUID

class TrainerConnection(
    private val uuid: UUID,
) {

    private val socket = SocketClient("127.0.0.1", 7654)

    init {
        socket.startReading { message ->
            when (message) {
                is LoadNetwork -> {
                    // TODO
                }
                is StartControl -> {
                    // TODO
                }
                is StopControl -> {
                    // TODO
                }
                else -> {}
            }
        }
    }

    fun connect() {
        Log.log(LogMessageType.NEAT, level = LogLevels.INFO) { "Registering on trainer as $uuid..." }
        socket.send(
            RegisterClient(
                uuid = uuid
            )
        )
    }

}