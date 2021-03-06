package bluebot;

import bluebot.commands.fun.*;
import bluebot.commands.fun.quickreactions.IDGFCommand;
import bluebot.commands.fun.quickreactions.KappaCommand;
import bluebot.commands.fun.quickreactions.NopeCommand;
import bluebot.commands.fun.quickreactions.WatCommand;
import bluebot.commands.misc.*;
import bluebot.commands.moderation.*;
import bluebot.commands.owner.AnnouncementCommand;
import bluebot.commands.owner.SetGameCommand;
import bluebot.commands.owner.SetOnlineStateCommand;
import bluebot.commands.owner.ShutDownCommand;
import bluebot.commands.utility.*;
import bluebot.utils.Command;
import bluebot.utils.CommandParser;
import bluebot.utils.LoadingProperties;
import bluebot.utils.SaveThread;
import bluebot.utils.listeners.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @file MainBot.java
 * @author Blue
 * @version 0.45 (I forgot some ...)
 * @brief The main class of BlueBot
 */

public class MainBot {


    private static JDA jda;
    private static LoadingProperties config = new LoadingProperties();
    private static AudioPlayerManager playerManager;


    private static String botOwner;
    private static LocalDateTime startTime = LocalDateTime.now();
    public static final CommandParser parser = new CommandParser();
    public static  Map<String, Command> commands = new TreeMap<String, Command>();
    private static Map<String, String> streamerList =  new HashMap<>();
    private static Map<String, String> autoRoleList = new HashMap<>();

    private static Map<String, ArrayList<String>> badWords = new HashMap<>();
    private static Map<String, String> prefixes = new HashMap<>();
    private static Map<String, String> bannedServers = new HashMap<>(); //server, reason for ban

    private static ArrayList<String> twitchDisabled = new ArrayList<>();
    private static ArrayList<String> cleverBotDisabled = new ArrayList<>();
    private static ArrayList<String> bwDisabled = new ArrayList<>();
    private static ArrayList<String> userEventDisabled = new ArrayList<>();
    private static ArrayList<String> nameProtectionDisabled = new ArrayList<>();


    private static Map<String, String> twitchChannel = new HashMap<>();
    private static Map<String, String> userEventChannel = new HashMap<>();
    private static Map<String, String> musicChannel = new HashMap<>();

    public static Map<String, String> getBannedServers() {
        return bannedServers;
    }
    public static String getBotOwner() {
        return botOwner;
    }
    public static ArrayList<String> getTwitchDisabled() {
        return twitchDisabled;
    }
    public static ArrayList<String> getCleverBotDisabled() {
        return cleverBotDisabled;
    }
    public static ArrayList<String> getBwDisabled() {
        return bwDisabled;
    }
    public static ArrayList<String> getUserEventDisabled() {
        return userEventDisabled;
    }
    public static Map<String, String> getTwitchChannel() {
        return twitchChannel;
    }
    public static Map<String, String> getUserEventChannel() {
        return userEventChannel;
    }
    public static Map<String, String> getMusicChannel() {
        return musicChannel;
    }

    private static String basePrefix = "!";

    public static void handleCommand(CommandParser.CommandContainer cmdContainer) {
        if(commands.containsKey(cmdContainer.invoke)) {
            boolean safe = commands.get(cmdContainer.invoke).called(cmdContainer.args, cmdContainer.event);
            if(safe) {
                commands.get(cmdContainer.invoke).action(cmdContainer.args, cmdContainer.event);
                commands.get(cmdContainer.invoke).executed(safe, cmdContainer.event);
            }
            else {
                commands.get(cmdContainer.invoke).executed(safe, cmdContainer.event);
            }
        }
    }


    public MainBot() {
        try {
            //jda instanciation
            //default method as provided in the API
            config = new LoadingProperties();
            SaveThread saveThread = new SaveThread();

            playerManager = new DefaultAudioPlayerManager();
            AudioSourceManagers.registerLocalSource(playerManager);

            jda = new JDABuilder(AccountType.BOT).setToken(config.getBotToken())
                    .addEventListener(new TwitchListener())
                    //.addListener(new CleverbotListener())
                    .addEventListener(new BadWordsListener())
                    .addEventListener(new UserJoinLeaveListener())
                    .addEventListener(new BotKickedListener())
                    .addEventListener(new BannedServersListener())
                    .addEventListener(new MessageReceivedListener())
                    .setBulkDeleteSplittingEnabled(false).buildBlocking();

            botOwner = config.getBotOwner();
            jda.getPresence().setGame(Game.of(config.getBotActivity()));
            System.out.println("Current activity " + jda.getPresence().getGame());

            //Loading the previous state of the bot(before shutdown)
            Gson gson = new Gson();
            streamerList = gson.fromJson(config.getStreamerList(), new TypeToken<Map<String, String>>(){}.getType());
            autoRoleList = gson.fromJson(config.getAutoRoleList(), new TypeToken<Map<String, String>>(){}.getType());

            badWords = gson.fromJson(config.getBadWords(), new TypeToken<Map<String, ArrayList<String>>>(){}.getType());
            prefixes = gson.fromJson(config.getPrefixes(), new TypeToken<Map<String, String>>(){}.getType());

            twitchDisabled = gson.fromJson(config.getTwitchDisabled(), new TypeToken<ArrayList<String>>(){}.getType());
            cleverBotDisabled = gson.fromJson(config.getCleverBotDisabled(), new TypeToken<ArrayList<String>>(){}.getType());
            bwDisabled = gson.fromJson(config.getBwDisabled(), new TypeToken<ArrayList<String>>(){}.getType());
            userEventDisabled = gson.fromJson(config.getUserEventDisabled(), new TypeToken<ArrayList<String>>(){}.getType());

            twitchChannel = gson.fromJson(config.getTwitchChannel(), new TypeToken<Map<String, String>>(){}.getType());
            userEventChannel = gson.fromJson(config.getUserEventChannel(), new TypeToken<Map<String, String>>(){}.getType());
            musicChannel = gson.fromJson(config.getMusicChannel(), new TypeToken<Map<String, String>>(){}.getType());

            saveThread.run();

            System.out.println("Connected servers : " + jda.getGuilds().size());
            System.out.println("Concerned users : " + jda.getUsers().size());

        } catch (InterruptedException | LoginException | RateLimitedException e) {
         System.out.println("No internet connection or invalid or missing token. Please edit config.blue and try again.");
        }

        //Banned servers
        bannedServers.put("304031909648793602", "spamming w/ bots to crash small bots");
        //bannedServers.put("281978005088370688", "BlueBot TestServer");

        //Activated bot commands
        commands.put("ping", new PingCommand());
        commands.put("sayhi", new SayHiCommand());
        commands.put("say", new SayCommand());
        commands.put("rate", new RateCommand());
        //commands.put("clear", new ClearCommand());
        commands.put("whoareyou", new WhoAreYouCommand());
        commands.put("help", new HelpCommand());
        commands.put("nope", new NopeCommand());
        commands.put("ymjoke", new YoMommaJokeCommand());
        commands.put("wat", new WatCommand());
        commands.put("gif", new GifCommand());
        commands.put("c&h", new CyanideHapinessCommand());
        commands.put("xkcd", new XKCDCommand());
        commands.put("setprefix", new SetPrefixCommand());
        commands.put("sound", new PlaySoundCommand());
        commands.put("tracktwitch", new TrackTwitchCommand());
        commands.put("untrack", new UntrackCommand());
        commands.put("setautorole", new SetAutoRoleCommand());
        commands.put("cat", new CatCommand());
        commands.put("dog", new DogCommand());
        commands.put("idgf", new IDGFCommand());
        commands.put("bw", new BadWordCommand());
        commands.put("info", new InfoCommand());
        commands.put("steam", new SteamStatusCommand());
        commands.put("kappa", new KappaCommand());
        commands.put("enable", new EnableListenerCommand());
        commands.put("disable", new DisableListenerCommand());
        commands.put("channel", new SpecificChannelCommand());
        commands.put("invite", new InviteCommand());
        commands.put("call", new CallCommand());
        commands.put("whois", new WhoisCommand());
        commands.put("github", new GitHubCommand());
        //commands.put("prune", new PruneCommand());

        //Owner commands
        commands.put("setgame", new SetGameCommand());
        commands.put("setos", new SetOnlineStateCommand());
        commands.put("shutdown", new ShutDownCommand());
        commands.put("announce", new AnnouncementCommand());
    }

    public static JDA getJda() {return jda;}
    public static LoadingProperties getConfig() {
        return config;
    }
    public static AudioPlayerManager getPlayerManager() {return playerManager;}
    public static Map<String, String> getStreamerList() {return streamerList;}
    public static Map<String, String> getAutoRoleList() {return autoRoleList;}
    public static LocalDateTime getStartTime() {return startTime;}
    public static Map<String, ArrayList<String>> getBadWords() {return badWords;}
    public static String getBasePrefix() {return basePrefix;}
    public static Map<String, String> getPrefixes() {return prefixes;}
    public static ArrayList<String> getNameProtectionDisabled() {return nameProtectionDisabled;}
}