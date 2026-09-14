package dev.hubpilot.core;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Same reflective wire contract and assertions run against published and rebuilt Core. */
public final class SettingsSyncCharacterization {
    public static final class Config {
        final Path directory; int reloads;
        Config(Path directory) { this.directory=directory; }
        public Snapshot snapshot() { return new Snapshot(directory); }
        public void reload() { reloads++; }
    }
    public record Snapshot(Path directory) {
        public Collection<String> trustedRequestServers() { return List.of("hub"); }
        public String sharedDirectory() { return directory.toString(); }
    }
    public record Source(String name) { public Source getServerInfo() { return this; } public String getName() { return name; } }
    public static final class Event {
        final String channel; final Object source; final byte[] data; boolean handled;
        Event(String channel,Object source,byte[] data) { this.channel=channel;this.source=source;this.data=data; }
        public MinecraftChannelIdentifier getIdentifier() { return MinecraftChannelIdentifier.from(channel); }
        public Object getSource() { return source; }
        public byte[] getData() { return data; }
        public void setResult(PluginMessageEvent.ForwardResult result) { handled=!result.isAllowed(); }
    }
    public static final class Log { public void warn(String text) {} public void info(String text) {} }
    static byte[] payload(String text) throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeUTF("SETTINGS_SNAPSHOT");out.writeUTF(text);return bytes.toByteArray();
    }
    static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    static Properties read(Path file) throws Exception { Properties p=new Properties();try(Reader r=Files.newBufferedReader(file)){p.load(r);}return p; }
    public static void main(String[] args) throws Exception {
        Path directory=Files.createTempDirectory("hubpilot-settings-characterization-");
        try {
            Config config=new Config(directory);SettingsSyncHook.initialize(config,new Log());Path file=directory.resolve("hubpilot.properties");
            check(!SettingsSyncHook.handle(new Event("hubpilot:status",new Source("hub"),new byte[0])),"Unrelated channel must not be swallowed");
            System.out.println("PASS settings-unrelated-channel-passes-through");
            Event valid=new Event("hubpilot:settings",new Source("hub"),payload("global.countdown-seconds=7\nserver.game.retry-count=2\nunsafe.key=secret\n"));
            check(SettingsSyncHook.handle(valid)&&valid.handled,"Trusted message must be handled");
            Properties props=read(file);check("7".equals(props.getProperty("global.countdown-seconds"))&&"2".equals(props.getProperty("server.game.retry-count"))&&"2".equals(props.getProperty("format-version"))&&!props.containsKey("unsafe.key"),"Validated snapshot lost or unrestricted key retained");
            check(config.reloads==1,"Applied snapshot reloads once");System.out.println("PASS settings-trusted-wire-snapshot-allowlist-and-reload");
            SettingsSyncHook.handle(valid);check(config.reloads==1,"Equal snapshot must not reload");System.out.println("PASS settings-equal-snapshot-no-reload");
            SettingsSyncHook.handle(new Event("hubpilot:settings",new Source("untrusted"),payload("global.countdown-seconds=99\n")));check(read(file).equals(props),"Untrusted snapshot changed state");System.out.println("PASS settings-untrusted-backend-rejected");
            SettingsSyncHook.handle(new Event("hubpilot:settings",new Object(),payload("global.countdown-seconds=99\n")));check(read(file).equals(props),"Client-like source changed state");System.out.println("PASS settings-client-origin-rejected");
            for(byte[] data:new byte[][]{new byte[0],new byte[65536],new byte[]{1}})SettingsSyncHook.handle(new Event("hubpilot:settings",new Source("hub"),data));check(read(file).equals(props),"Malformed payload changed state");System.out.println("PASS settings-empty-oversized-malformed-bounds");
            SettingsSyncHook.handle(new Event("hubpilot:settings",new Source("hub"),payload("global.countdown-seconds=9\n")));props=read(file);check("9".equals(props.getProperty("global.countdown-seconds"))&&!props.containsKey("server.game.retry-count")&&config.reloads==2,"Snapshot replacement contract changed");System.out.println("PASS settings-snapshot-replaces-not-merges");
            System.out.println("TOTAL PASS 7 settings transport cases");
        } finally { try(var paths=Files.walk(directory)){ for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path); } }
    }
}
