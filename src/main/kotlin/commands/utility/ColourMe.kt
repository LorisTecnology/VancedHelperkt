package commands.utility

import core.command.CommandContext
import core.command.base.BaseCommand
import core.database.allowedColourMeRoleIds
import core.wrapper.applicationcommand.CustomApplicationCommandCreateBuilder
import core.wrapper.applicationcommand.CustomApplicationCommandPermissionBuilder
import dev.kord.common.entity.Snowflake
import dev.kord.common.kColor
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.edit
import dev.kord.core.entity.interaction.string
import dev.kord.rest.Image
import dev.kord.rest.builder.interaction.string
import io.ktor.client.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.koin.core.component.inject
import java.awt.Color

class ColourMe : BaseCommand(
    commandName = "colourme",
    commandDescription = "Create a custom role for yourself.",
    defaultPermissions = false
) {

    private val httpClient: HttpClient by inject()

    override suspend fun execute(
        ctx: CommandContext
    ) {
        val roleName = ctx.args["name"]!!.string()
        val roleColor = ctx.args["color"]!!.string()
        val iconUrl = ctx.args["icon"]?.string()

        val author = ctx.author

        val kordColor = Color.decode(roleColor).kColor

        val existingRole = author.roles.firstOrNull { it.name.contains("-CC") }

        val newRoleName = "$roleName-CC"

        if (existingRole != null) {
            existingRole.edit {
                if (iconUrl != null) {
                    try {
                        icon = Image.fromUrl(httpClient, iconUrl)
                    } catch (e: Exception) {
                        ctx.respondEphemeral {
                            content = "Failed to retrieve the icon from the provided URL"
                        }
                        return@edit
                    }
                }

                name = newRoleName
                color = kordColor
            }
        } else {
            val newRole = author.guild.createRole {
                if (iconUrl != null) {
                    try {
                        icon = Image.fromUrl(httpClient, iconUrl)
                    } catch (e: Exception) {
                        ctx.respondEphemeral {
                            content = "Failed to retrieve the icon from the provided URL"
                        }
                        return@createRole
                    }
                }
                name = newRoleName
                color = kordColor
            }
            val newPosition = author.roles.toList().sortedByDescending { it.rawPosition }.map { it.rawPosition }[0] + 1
            newRole.changePosition(newPosition)
            author.addRole(newRole.id, "colourme")
        }

        ctx.respondEphemeral {
            content = "Successfully assigned the $newRoleName role"
        }
    }

    override suspend fun commandOptions() =
        CustomApplicationCommandCreateBuilder(
            arguments = {
                string(
                    name = "name",
                    description = "Name of the role"
                ) {
                    required = true
                }
                string(
                    name = "color",
                    description = "Hex color of the role"
                ) {
                    required = true
                }
                string(
                    name = "icon",
                    description = "Icon URL for the role"
                ) {
                    required = false
                }
            }
        )

    override fun commandPermissions() =
        CustomApplicationCommandPermissionBuilder(
            permissions = {
                for (allowedColourMeRoleId in allowedColourMeRoleIds) {
                    role(
                        id = Snowflake(allowedColourMeRoleId),
                        allow = true
                    )
                }
            }
        )

}