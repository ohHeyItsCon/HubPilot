package dev.hubpilot.hub.config;

import java.util.Locale;
import java.util.Objects;

public final class Destination {
   private final String id;
   private final String label;
   private final String description;
   private final String software;
   private final String iconMaterial;
   private final Destination.TargetType targetType;
   private final String target;
   private final String statusTarget;
   private final boolean enabled;

   public Destination(String var1, String var2, String var3, String var4, String var5, Destination.TargetType var6, String var7, String var8, boolean var9) {
      this.id = normalizeId(var1);
      this.label = nonBlank(var2, this.id);
      this.description = nonBlank(var3, "Click to connect");
      this.software = nonBlank(var4, "Unknown");
      this.iconMaterial = normalizeIconName(var5);
      this.targetType = Objects.requireNonNull(var6, "targetType");
      this.target = validateTarget(var7);
      this.statusTarget = validateTarget(nonBlank(var8, this.target));
      this.enabled = var9;
   }

   public String id() {
      return this.id;
   }

   public String label() {
      return this.label;
   }

   public String description() {
      return this.description;
   }

   public String software() {
      return this.software;
   }

   public String iconMaterial() {
      return this.iconMaterial;
   }

   public Destination.TargetType targetType() {
      return this.targetType;
   }

   public String target() {
      return this.target;
   }

   public String statusTarget() {
      return this.statusTarget;
   }

   public boolean enabled() {
      return this.enabled;
   }

   public Destination withLabel(String var1) {
      return new Destination(this.id, var1, this.description, this.software, this.iconMaterial, this.targetType, this.target, this.statusTarget, this.enabled);
   }

   public Destination withDescription(String var1) {
      return new Destination(this.id, this.label, var1, this.software, this.iconMaterial, this.targetType, this.target, this.statusTarget, this.enabled);
   }

   public Destination withSoftware(String var1) {
      return new Destination(this.id, this.label, this.description, var1, this.iconMaterial, this.targetType, this.target, this.statusTarget, this.enabled);
   }

   public Destination withIconMaterial(String var1) {
      return new Destination(this.id, this.label, this.description, this.software, var1, this.targetType, this.target, this.statusTarget, this.enabled);
   }

   public Destination withTargetType(Destination.TargetType var1) {
      return new Destination(this.id, this.label, this.description, this.software, this.iconMaterial, var1, this.target, this.statusTarget, this.enabled);
   }

   public Destination withTarget(String var1) {
      return new Destination(this.id, this.label, this.description, this.software, this.iconMaterial, this.targetType, var1, this.statusTarget, this.enabled);
   }

   public Destination withStatusTarget(String var1) {
      return new Destination(this.id, this.label, this.description, this.software, this.iconMaterial, this.targetType, this.target, var1, this.enabled);
   }

   public Destination withEnabled(boolean var1) {
      return new Destination(this.id, this.label, this.description, this.software, this.iconMaterial, this.targetType, this.target, this.statusTarget, var1);
   }

   public static String normalizeId(String var0) {
      String var1 = var0 == null ? "" : var0.trim().toLowerCase(Locale.ENGLISH);
      if (!var1.matches("[a-z0-9_-]+")) {
         throw new IllegalArgumentException("ID may only contain lowercase letters, numbers, _ and -");
      } else {
         return var1;
      }
   }

   public static String validateTarget(String var0) {
      String var1 = var0 == null ? "" : var0.trim();
      if (!var1.matches("[A-Za-z0-9_.-]+")) {
         throw new IllegalArgumentException("Target may only contain letters, numbers, _, - and .");
      } else {
         return var1;
      }
   }

   private static String normalizeIconName(String var0) {
      String var1 = nonBlank(var0, "GRASS_BLOCK").toUpperCase(Locale.ENGLISH).replace(' ', '_');
      if (var1.startsWith("MINECRAFT:")) {
         var1 = var1.substring("MINECRAFT:".length());
      }

      return var1;
   }

   private static String nonBlank(String var0, String var1) {
      return var0 != null && !var0.trim().isEmpty() ? var0.trim() : var1;
   }

   public static enum TargetType {
      SERVER,
      WORLD;
   }
}
