package aspectibot;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord_commands.BirthdayCommand;
import discord_commands.YTVodCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;


public class DiscordServerListener extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(DiscordServerListener.class);
	private HashMap<String, DiscordCommand> commandMap = new HashMap<>();
	   
	public void onReady(ReadyEvent event) {
		//register slash commands
		ArrayList<CommandData> commands = new ArrayList<>();
		BirthdayCommand birthdayCommand = new BirthdayCommand();
		YTVodCommand ytvodCommand = new YTVodCommand();
		commands.add(birthdayCommand.register());
		commands.add(ytvodCommand.register());
		commandMap.put("birthday", birthdayCommand);
		commandMap.put("addytvod", ytvodCommand);

		event.getJDA().updateCommands().addCommands(commands).queue();
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		DiscordCommand cmd;
		if((cmd = commandMap.get(event.getName())) != null) {
			MessageCreateData data = cmd.reply(event);
			event.reply(data).setEphemeral(cmd.isEphemeral()).queue();
		}
	}
    
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message message = event.getMessage();
		Member m = event.getMember();
		if (m == null) {
            LOG.error("onMessageReceived: Member is null!");
            return;
        }
		
		GuildChannel channel = (event.getChannelType().isAudio()) ? 
									event.getChannel().asVoiceChannel() : 
									event.getChannel().asGuildMessageChannel();
		
		Guild guild = channel.getGuild();
		
		if (Long.valueOf(guild.getId()) == AspectiBot.SERVER_ID
				&& !event.getAuthor().isBot()) {
			buildEmbed(message, m, channel, guild, false);
		}
		
	}
	
	@Override
	public void onMessageUpdate(MessageUpdateEvent event) {
		
		Message message = event.getMessage();
		Member m = event.getMember();
		if(m == null) {
			LOG.error("onMessageUpdate: Member is null!");
			return;
		}

		GuildChannel channel = (event.getChannelType().isAudio()) ? 
									event.getChannel().asVoiceChannel() : 
									event.getChannel().asGuildMessageChannel();
		
		Guild guild = channel.getGuild();
		
		if(Long.valueOf(guild.getId()) == AspectiBot.SERVER_ID
				&& !event.getAuthor().isBot()) {
			buildEmbed(message, m, channel, guild, true);
		}
	}

	private void buildEmbed(Message message, Member m, GuildChannel channel, Guild guild, boolean edited) {
		EmbedBuilder logMessage = new EmbedBuilder();
			
			logMessage.setTitle(m.getEffectiveName(), message.getJumpUrl());
			logMessage.setDescription("#" + channel.getName());

			String name = edited ? "edited:" : "posted:";
			
			if (message.getContentDisplay().length() >= 1024) {
				logMessage.addField(
					name, 
					message.getContentDisplay().substring(0, 1021) + "...", 
					true);
			} else {
				logMessage.addField(
					name, 
					message.getContentDisplay(), 
					true);
			}
			
			if (!edited) {
				logMessage.setTimestamp(message.getTimeCreated());
			} else {
				logMessage.setTimestamp(message.getTimeEdited());
				DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
				logMessage.setFooter("original message posted: " + message.getTimeCreated().format(formatter));
			}
			
			if (!message.getAttachments().isEmpty()) {
				// for (int i = 0; i < message.getAttachments().size(); i++) {
					int i = 0; // TODO: I assume we would need to send a new message for each attachment
					// probably would have to be async? mind the rate limit
					// maybe too much work
					if (message.getAttachments().get(i).isImage()) {
						logMessage.addField(
							"uploaded image:", 
							"", 
							false);
						logMessage.setImage(message.getAttachments().get(i).getUrl());
					} else if (message.getAttachments().get(i).isVideo()) {
						logMessage.addField(
							"uploaded video:", 
							message.getAttachments().get(i).getUrl(), 
							false);
					}
				// }
			}
			
			logMessage.setColor(0xf705ff);
			TextChannel logChannel = guild.getTextChannelById(AspectiBot.LOG_CHANNEL_ID);
			if (logChannel != null) {
				logChannel.sendMessageEmbeds(logMessage.build()).queue();
			} else {
				LOG.error("onMessageReceived: logChannel is null!");
			}
			logMessage.clear();
	}

	@Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        
        // get the member who joined
        Member member = event.getMember();
        
        // give member who joined the default role
        Role defaultRole = event.getJDA().getRoleById(AspectiBot.DEFAULT_ROLE);
        if(defaultRole != null) {
            event.getMember().getGuild().addRoleToMember(member, defaultRole).queue();
        } else {
            LOG.error("onGuildMemberJoin: Default role not configured or invalid!");
        }

    } // end of onGuildMemberJoin method
}
