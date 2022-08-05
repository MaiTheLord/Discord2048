import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object SetupCommand : SlashCommand {
    override val name = "setup"
    override val description = "Setup a channel for the bot"

    override fun onCommand(event: SlashCommandInteractionEvent) {
        println("Setup command called in ${event.guild?.name}")
    }
}
