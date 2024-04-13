package aspectibot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public interface DiscordCommand {
    public boolean isEphemeral = false;
    public CommandData register();
    public MessageCreateData reply(SlashCommandInteractionEvent event);
    public MessageCreateBuilder error();
}
