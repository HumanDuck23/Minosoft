/*
 * Minosoft
 * Copyright (C) 2020-2025 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.terminal.commands

import de.bixilon.kutil.concurrent.pool.DefaultThreadPool
import de.bixilon.kutil.observer.DataObserver.Companion.observe
import de.bixilon.minosoft.commands.nodes.ArgumentNode
import de.bixilon.minosoft.commands.nodes.LiteralNode
import de.bixilon.minosoft.commands.parser.brigadier._int.IntParser
import de.bixilon.minosoft.commands.parser.brigadier.string.StringParser
import de.bixilon.minosoft.commands.stack.print.PrintTarget
import de.bixilon.minosoft.config.profile.profiles.account.AccountProfileManager
import de.bixilon.minosoft.data.accounts.types.offline.OfflineAccount
import de.bixilon.minosoft.protocol.address.ServerAddress
import de.bixilon.minosoft.protocol.network.NetworkConnection
import de.bixilon.minosoft.protocol.network.session.play.PlaySession
import de.bixilon.minosoft.protocol.network.session.status.StatusSession
import de.bixilon.minosoft.protocol.versions.Version

object ConnectBotsCommand : Command {
    override var node = LiteralNode("bots")
        .addChild(ArgumentNode("count", IntParser(min = 1), allowArguments = true) { stack ->
            val count = stack.get<Int>("count")!!
            val address = stack.get<String>("address") ?: "localhost:25565"

            DefaultThreadPool += {
                stack.print.print("Pinging server to get version...")
                val ping = StatusSession(address)
                ping::status.observe(this) { connectAll(stack.print, ping.connection!!.address, ping.serverVersion ?: throw IllegalArgumentException("Could not determinate server's version!"), count) }
                ping::error.observe(this) { stack.print.print("Could not ping $address: $it") }
                ping.ping()
            }
        }.addChild(ArgumentNode("address", StringParser(StringParser.StringModes.QUOTED), executable = true)))


    private fun connectAll(print: PrintTarget, address: ServerAddress, version: Version, count: Int) {
        val profile = AccountProfileManager.selected
        for (i in 1..count) {
            val name = "Bot$i"
            val account = profile.entries[name] as? OfflineAccount ?: OfflineAccount(name, profile.storage).also { profile.entries[it.id] = it }
            print.print("Connecting §e$name§r to $address")
            PlaySession(NetworkConnection(address, true), account, version, headless = true).connect()
        }
    }
}
