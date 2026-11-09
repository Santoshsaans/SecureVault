package src.storage;

import src.crypto.KeyDerivation;
import src.crypto.VaultCrypto;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class VaultFile {

    private final Path vaultPath;
    private byte[] salt;
    private SecretKey key;

    public VaultFile(String path) {
        this.vaultPath = Paths.get(path);
    }

    public boolean exists() {
        return Files.exists(vaultPath);
    }

    public void createNew(char[] masterPassword, List<VaultEntry> entries) throws Exception {
        this.salt = KeyDerivation.generateSalt();
        this.key = KeyDerivation.deriveKey(masterPassword, salt);
        writeEntries(entries);
    }

    public List<VaultEntry> unlock(char[] masterPassword) throws Exception {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(vaultPath.toFile())))) {
            int saltLen = in.readInt();
            byte[] s = new byte[saltLen];
            in.readFully(s);

            int ivLen = in.readInt();
            byte[] iv = new byte[ivLen];
            in.readFully(iv);

            byte[] ciphertext = in.readAllBytes();

            SecretKey k = KeyDerivation.deriveKey(masterPassword, s);
            byte[] plaintext = VaultCrypto.decrypt(ciphertext, k, iv);

            // Only cache on success (wrong password throws above)
            this.salt = s;
            this.key = k;
            return deserialize(new String(plaintext, StandardCharsets.UTF_8));
        }
    }

    public void save(List<VaultEntry> entries) throws Exception {
        if (key == null || salt == null) {
            throw new IllegalStateException("Vault is locked; unlock before saving.");
        }
        writeEntries(entries);
    }

    private void writeEntries(List<VaultEntry> entries) throws Exception {
        byte[] iv = VaultCrypto.generateIV();
        byte[] ciphertext = VaultCrypto.encrypt(
            serialize(entries).getBytes(StandardCharsets.UTF_8), key, iv);

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(vaultPath.toFile())))) {
            out.writeInt(salt.length);
            out.write(salt);
            out.writeInt(iv.length);
            out.write(iv);
            out.write(ciphertext);
        }

        try {
            Files.setPosixFilePermissions(vaultPath,
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {}
    }

    private String serialize(List<VaultEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (VaultEntry e : entries) {
            sb.append(escape(e.getSite())).append("|")
              .append(escape(e.getUsername())).append("|")
              .append(escape(e.getPassword())).append("|")
              .append(escape(e.getNotes())).append("\n");
        }
        return sb.toString();
    }

    private List<VaultEntry> deserialize(String data) {
        List<VaultEntry> list = new ArrayList<>();
        for (String line : data.split("\n")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", -1);
            if (parts.length < 4) continue;
            list.add(new VaultEntry(unescape(parts[0]), unescape(parts[1]),
                                    unescape(parts[2]), unescape(parts[3])));
        }
        return list;
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("|", "\\|").replace("\n", "\\n");
    }

    private String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\|", "|").replace("\\\\", "\\");
    }
}