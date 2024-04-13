package discord_commands;

import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aspectibot.AspectiBot;
import aspectibot.DiscordCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class YTVodCommand implements DiscordCommand {

	private static final Logger LOG = LoggerFactory.getLogger(AspectiBot.class);
    public static boolean isEphemeral = true;

    @Override
    public CommandData register() {
        CommandData command = Commands.slash("addytvod", "add youtube vod button to message")
            .addOptions(
                new OptionData(OptionType.STRING, "message-id", "id of the message to attach the link to", true),
                new OptionData(OptionType.STRING, "url", "youtube link of the vod", true)
            )
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_CHANNEL));
        return command;
    }

    @Override
    public MessageCreateData reply(SlashCommandInteractionEvent event) {
        String message_id = event.getOption("message-id").getAsString();
        String url = event.getOption("url").getAsString();
        try {
            Message msg = AspectiBot.jda.getNewsChannelById(AspectiBot.LIVE_CHANNEL_ID)
                .retrieveMessageById(message_id)
                .submit()
                .get();
            LayoutComponent url_button = (LayoutComponent) Button.link(url, "Watch VOD on YT");
            msg.editMessageComponents(url_button).queue();
        } catch (InterruptedException e1) {
            LOG.error("reply: unable to get the message!");
            e1.printStackTrace();
            error();
        } catch (ExecutionException e2) {
            LOG.error("reply: insufficient permissions getting message!");
            e2.printStackTrace();
            error();
        }
        return new MessageCreateBuilder().setContent("changed the link!").build();
    }

    @Override
    public MessageCreateBuilder error() {
        MessageCreateBuilder message = new MessageCreateBuilder()
            .setContent("unable to add button to the VOD!");
        return message;
    }
    
}
