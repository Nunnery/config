/**
 * The MIT License
 * Copyright (c) 2015 Teal Cube Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.tealcube.minecraft.bukkit.config;

import com.github.zafarkhaja.semver.Version;
import com.google.common.io.Files;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * An extension of SmartYamlConfiguration that can backup and update itself.
 */
public class VersionedSmartYamlConfiguration extends SmartYamlConfiguration implements VersionedConfiguration {

    private static final String YAML_ENDING = ".yml";
    private static final String BACKUP_ENDING = ".backup";
    private Configuration checkAgainst;
    private VersionUpdateType updateType;

    /**
     * Instantiates a new VersionedIvoryYamlConfiguration with a selected {@link java.io.File} to load/save from/to, a
     * {@link java.io.File} to check against, and an {@link VersionedConfiguration.VersionUpdateType}.
     *
     * @param file         file to load/save from/to
     * @param checkAgainst file to check against
     * @param updateType   type of updating
     */
    public VersionedSmartYamlConfiguration(File file, File checkAgainst,
                                           VersionUpdateType updateType) {
        this(file, '.', checkAgainst, updateType);
    }

    /**
     * Instantiates a new VersionedIvoryYamlConfiguration with a selected {@link java.io.File} to load/save from/to, a
     * {@link java.io.File} to check against, and an {@link VersionedConfiguration.VersionUpdateType}.
     *
     * @param file         file to load/save from/to
     * @param separator    character to separate file sections on
     * @param checkAgainst file to check against
     * @param updateType   type of updating
     */
    public VersionedSmartYamlConfiguration(File file, char separator, File checkAgainst,
                                           VersionUpdateType updateType) {
        super(file, separator);
        if (checkAgainst != null && checkAgainst.exists()) {
            this.checkAgainst = new SmartYamlConfiguration(checkAgainst);
        }
        this.updateType = updateType;
    }

    /**
     * Instantiates a new VersionedIvoryYamlConfiguration with a selected {@link java.io.File} to load/save from/to, a
     * {@link java.io.InputStream} to check against, and an {@link VersionedConfiguration.VersionUpdateType}.
     *
     * @param file         file to load/save from/to
     * @param checkAgainst resource to check against
     * @param updateType   type of updating
     */
    public VersionedSmartYamlConfiguration(File file, InputStream checkAgainst,
                                           VersionUpdateType updateType) {
        this(file, '.', checkAgainst, updateType);
    }

    /**
     * Instantiates a new VersionedIvoryYamlConfiguration with a selected {@link java.io.File} to load/save from/to, a
     * {@link java.io.InputStream} to check against, and an {@link VersionedConfiguration.VersionUpdateType}.
     *
     * @param file         file to load/save from/to
     * @param separator    character to separate file sections on
     * @param checkAgainst resource to check against
     * @param updateType   type of updating
     */
    public VersionedSmartYamlConfiguration(File file, char separator, InputStream checkAgainst,
                                           VersionUpdateType updateType) {
        super(file, separator);
        if (checkAgainst != null) {
            this.checkAgainst = YamlConfiguration.loadConfiguration(checkAgainst);
        }
        this.updateType = updateType;
    }

    public VersionedSmartYamlConfiguration(Configuration configuration, Configuration checkAgainst,
                                           VersionUpdateType updateType) {
        super(configuration);
        if (checkAgainst != null) {
            this.checkAgainst = checkAgainst;
        }
        this.updateType = updateType;
    }

    /**
     * Gets and returns the version passed into the constructor.
     *
     * @return version passed into the constructor
     */
    @Override
    public String getVersion() {
        return checkAgainst == null ? "" : checkAgainst.getString("version", "");
    }

    /**
     * Gets and returns the version contained in the actual YAML file.
     *
     * @return version in the YAML file
     */
    @Override
    public String getLocalVersion() {
        return getString("version", "");
    }

    /**
     * Returns true if this file needs to update itself and false if not.
     *
     * @return if file needs to update
     */
    @Override
    public boolean needsToUpdate() {
        Version version = Version.valueOf(getVersion());
        Version localVersion = Version.valueOf(getLocalVersion());
        return localVersion.greaterThan(version);
    }

    /**
     * Attempts to update itself and returns if it succeeded.
     *
     * @return if update was successful
     */
    @Override
    public boolean update() {
        if (!needsToUpdate()) {
            return false;
        }
        File directory = getFile().getParentFile();
        File backupLocation = new File(directory, getFile().getName().replace(YAML_ENDING, YAML_ENDING + BACKUP_ENDING));
        switch (updateType) {
            case BACKUP_NO_UPDATE:
                try {
                    Files.copy(getFile(), backupLocation);
                } catch (IOException e) {
                    return false;
                }
                return true;
            case BACKUP_AND_UPDATE:
                try {
                    Files.copy(getFile(), backupLocation);
                } catch (IOException e) {
                    return false;
                }
                for (String key : checkAgainst.getKeys(true)) {
                    if (checkAgainst.isConfigurationSection(key)) {
                        continue;
                    }
                    if (!isSet(key)) {
                        set(key, checkAgainst.get(key));
                    }
                }
                set("version", getVersion());
                save();
                return true;
            case BACKUP_AND_NEW:
                try {
                    Files.copy(getFile(), backupLocation);
                } catch (IOException e) {
                    return false;
                }
                for (String key : getKeys(true)) {
                    set(key, null);
                }
                for (String key : checkAgainst.getKeys(true)) {
                    if (checkAgainst.isConfigurationSection(key)) {
                        continue;
                    }
                    set(key, checkAgainst.get(key));
                }
                set("version", getVersion());
                save();
                return true;
            case NOTHING:
                return true;
            default:
                return true;
        }
    }


}
