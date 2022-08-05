import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity

private fun main() {
    JDABuilder
        .createDefault(Config.token)
        .setActivity(Activity.playing("10 - Its 2048, but tiny!"))
        .addEventListeners(SlashCommands, ChannelManager)
        .build()
}
