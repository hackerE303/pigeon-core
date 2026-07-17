Pigeon Core is built from the ground up to support a unified, interconnected ecosystem where different mods using the framework can securely interact and communicate. By treating the registry system like an accessible centralized map, any mod can safely query or hook into elements registered by peer mods, facilitating smooth cross-mod integrations.

To manage this safely, all elements provided by Pigeon Core expose two critical identification methods: `pigeid()` and `modid()`. Overriding these methods allows developers to fine-tune how their custom elements are recognized by the framework's runtime engines.

## Identification Methods

When extending an element provided by Pigeon Core you have direct access to these lifecycle hooks.

> ⚠️ **Important Lifecycle Behavior (Single Init)** These methods are invoked by the framework exactly **once** during the early registry bootstrap phase. The returned strings are immediately cached and handled as internal `final` values. Consequently, you cannot dynamically change these values at runtime after the mod has initialized.

### `pigeid()`

- **Purpose:** Defines the specific registry path/key identifier for your object.

- **Behavior:** By default, this maps to the value specified in your `@AutoRegister` annotation. Overriding this method allows you to change the `@AutoRegister` content.

### `modid()`

- **Purpose:** Controls the namespace target for the framework's automatic runtime asset and logic lookup behaviors.

- **Behavior:** By default, this automatically returns your own mod's ID. Overriding this method is highly useful when building official addons or extensions for another mod.

ℹ️ **Important Distinction:** Overriding `modid()` does not hijack or override the actual game registry namespace. The object is still securely registered under your mod's technical registry envelope. Instead, it signals to Pigeon Core's automatic data-driven systems to pull specialized behaviors and resources from the specified target mod.

Here is a practical example of a mod creating a specialized addon item designed to bridge data and behavior with another framework-compliant mod:

```
package com.exampleaddon.item;  
  
import software.hacker\_E303.pigeon\_core.EItem;   
import software.hacker\_E303.pigeon\_core.main.AutoRegister;  
  
@AutoRegister("alloy\_casing")  
public class ModExtensionItem extends EItem \{  
  
    /\*\*  
     \* Overriding pigeid changes the internal registry key name.  
     \* The item will register as 'exampleaddon:advanced\_alloy\_casing'.  
     \*/  
    @Override  
    public String pigeid() \{  
        return "advanced\_alloy\_casing";  
    \}  
  
    /\*\*  
     \* Overriding modid instructs automatic runtime systems to look   
     \* inside a parent mod's workspace for assets.  
     \*/  
    @Override  
    public String modid() \{  
        return "parent\_industrial\_mod";  
    \}  
\}
```

