package discord_commands;

import java.io.IOException;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aspectibot.AspectiBot;
import aspectibot.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import utils.JSONUtils;

public class BirthdayCommand implements DiscordCommand {

    private static final Logger LOG = LoggerFactory.getLogger(BirthdayCommand.class);
    public static boolean isEphemeral = true;

    public BirthdayCommand() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(BirthdayScheduler.TIME_ZONE));
        calendar.set(Calendar.HOUR_OF_DAY, BirthdayScheduler.HOUR);
        calendar.set(Calendar.MINUTE, BirthdayScheduler.MINUTE);
        calendar.set(Calendar.SECOND, BirthdayScheduler.SECOND);
        Date pingTime = calendar.getTime();
        Timer timer = new Timer();
        timer.schedule(new BirthdayScheduler(), pingTime);
    }

    @Override
    public CommandData register() {
        CommandData command = Commands.slash("birthday", "add your birthday to be pinged by AspectiBot!")
            .addSubcommands(
                new SubcommandData("add", "add your birthday to be pinged!")
                    .addOptions(
                        new OptionData(OptionType.STRING, "month", "the month you were born in")
                            .addChoice("January", "1")
                            .addChoice("February", "2")
                            .addChoice("March", "3")
                            .addChoice("April", "4")
                            .addChoice("May", "5")
                            .addChoice("June", "6")
                            .addChoice("July", "7")
                            .addChoice("August", "8")
                            .addChoice("September", "9")
                            .addChoice("October", "10")
                            .addChoice("November", "11")
                            .addChoice("December", "12")
                            .setRequired(true),
                        new OptionData(OptionType.INTEGER, "day", "the day you were born on")
                            .setRequiredRange(1, 31)                  
                            .setRequired(true)
                    ),
                new SubcommandData("remove", "remove yourself from being pinged on your birthday")
            );
        return command; 
    }

    @Override
    public MessageCreateData reply(SlashCommandInteractionEvent event) {
        String operation = event.getSubcommandName();
        MessageCreateBuilder message;
        if(operation.equalsIgnoreCase("add")) message = addBirthday(event);
        else if(operation.equalsIgnoreCase("remove")) message = removeBirthday(event);
        else message = error();
        return message.build();
    }

    public MessageCreateBuilder error() {
        MessageCreateBuilder message = new MessageCreateBuilder()
            .setContent("unable to process your request, try again later! (also blame onteia)");
        return message;
    }

    private MessageCreateBuilder addBirthday(SlashCommandInteractionEvent event) {
        String month;
        String day;
        String userID;
        try {
            month = event.getOption("month").getAsString();
            day = event.getOption("day").getAsString();
            userID = event.getUser().getId();
        } catch (IllegalArgumentException e) {
            LOG.error("addBirthday: unable to get command option!", e);
            return error();
        }

        try {
            JSONUtils.get(userID, AspectiBot.BIRTHDAY_LOG_PATH);
            LOG.info("addBirthday: duplicate birthday found!");
            MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("your birthday has already been added! make sure to use `/birthday remove` to remove your saved birthday");
            return message;
        } catch (JSONException e) {
            // birthday is new
        } catch (IOException e) {
            LOG.error("addBirthday: unable to check duplicate!", e);
            return error();
        }

        try {
            JSONUtils.append(month + "-" + day, userID, AspectiBot.BIRTHDAY_LOG_PATH);
            // add inverse for easy duplication checking
            JSONUtils.put(userID, month+"-"+day, AspectiBot.BIRTHDAY_LOG_PATH);
        } catch (IOException e) {
            LOG.error("addBirthday: unable to add birthday to json file!", e);
            return error();
        }

        MessageCreateBuilder message = new MessageCreateBuilder()
            .setContent("added your birthday!");
        LOG.info("added " + event.getUser().getEffectiveName() + " [" + event.getUser().getId() + "]'s birthday!");
        return message;
    }

    private MessageCreateBuilder removeBirthday(SlashCommandInteractionEvent event) {
        try {
            String userID = event.getUser().getId();
            String birthdate = JSONUtils.get(userID, AspectiBot.BIRTHDAY_LOG_PATH);
            JSONUtils.delete(userID, AspectiBot.BIRTHDAY_LOG_PATH);
            JSONArray array = JSONUtils.getArray(birthdate, AspectiBot.BIRTHDAY_LOG_PATH);
            
            int index = array.toList().indexOf(userID);
            array.remove(index);
            JSONUtils.addArray(birthdate, array, AspectiBot.BIRTHDAY_LOG_PATH);
        } catch (JSONException e) {
            MessageCreateBuilder message = new MessageCreateBuilder()
                .setContent("your birthday hasn't been set yet! make sure to use `/birthday add` to add your birthday");
            return message;
        } catch (IOException e) {
            LOG.error("removeBirthday: unable to read json file!", e);
            return error();
        }

        MessageCreateBuilder message = new MessageCreateBuilder()
            .setContent("removed your birthday from the ping list!");
        return message;
    }

}
