# SecureVault: A Java Password Manager

SecureVault is a desktop password manager built with **Java 21 and Swing**. You remember one master password, and SecureVault keeps all your other passwords encrypted in a single file.

> **Educational project.** It has not been security-audited. Do not store real credentials in it.

---

## What It Does

- You create a **master password** the first time you run it.
- You save website logins (site, username, password) inside the safe.
- Everything is encrypted and stored in one file: `vault.dat`.
- Without the master password, `vault.dat` is just unreadable data.

## Features

### Security

| Feature | Details |
|---|---|
| Key derivation | PBKDF2-HMAC-SHA256, 600,000 iterations |
| Salt | Random 16 bytes, new for every vault |
| Encryption | AES-256-GCM (encrypts and detects tampering) |
| IV (nonce) | Fresh random IV on every save |
| Clipboard safety | Copied passwords are cleared after 30 seconds |
| Auto-lock | Vault locks after 5 minutes of inactivity |
| Master password check | Weak master passwords are rejected on creation |

### Usability

| Feature | Details |
|---|---|
| Create / Unlock screens | Simple dialogs for setup and daily use |
| Table view | See all entries, with search/filter |
| Entry actions | Add, delete, copy password |
| Password generator | Creates strong random passwords |
| Strength meter | Live feedback while you type |
| Console mode | Works without a GUI |

## Requirements

- **Java 21** or newer (JDK, not just JRE)
- Windows, macOS, or Linux

## How to Run

### Compile

**Windows (PowerShell):**
```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })
```

**macOS / Linux:**
```bash
javac -d out $(find src -name "*.java")
```

### Run

**GUI mode:**
```
java -cp out src.Main
```

**Console mode:**
```
java -cp out src.Main console
```

## How to Use

1. Start the app.
2. Choose a **master password**. The strength meter must show it as strong enough.
3. Your vault file `vault.dat` is created.
4. Use the buttons to manage entries: **Add**, **Generate**, **Copy**, **Delete**, **Search**.

**Important:** If you forget the master password, the data cannot be recovered. There is no reset option. Keep a backup copy of `vault.dat` somewhere safe.

## How the Security Works

### Saving the vault

```
Master password + random salt
        |
        v
   PBKDF2 (600,000 rounds)
        |
        v
   256-bit AES key
        |
        v
 AES-GCM encrypt (with random IV)
        |
        v
 vault.dat = salt + IV + encrypted data
```

### Unlocking the vault

1. Read the salt and IV from `vault.dat`.
2. Rebuild the AES key from the master password and the salt.
3. Decrypt with AES-GCM.
4. If the password is wrong or the file was changed, GCM authentication fails and the vault stays locked.

### Why each part matters

| Part | Why it is used |
|---|---|
| PBKDF2 + 600,000 iterations | Makes every password guess slow, hurting brute-force attacks |
| Random salt | Stops attackers from using pre-built hash tables |
| AES-256 | Strong, widely trusted encryption |
| GCM mode | Encrypts and also detects tampering |
| Random IV | Prevents patterns in repeated saves |
| SecureRandom | Cryptographically safe randomness |

## Why Encryption, Not Hashing?

Hashing is one-way — you can verify a password but never get it back. That is perfect for **login systems**, where you only need to check "does this input match the stored hash?".

But a password manager must **retrieve** the original password to copy-paste it into a website. A hash would be useless — you would see `$2a$10$...` and have no way to paste it into Gmail.

So SecureVault uses **PBKDF2** (a hash-based function) to turn the master password into a strong AES key, and **AES-GCM** to encrypt the actual passwords so they can be decrypted when needed. This is the same split real password managers use — Bitwarden, 1Password, and KeePass all combine a KDF with symmetric encryption.

## Project Structure

```
SecureVault/
+-- src/
|   +-- Main.java                         # Entry point
|   +-- crypto/
|   |   +-- KeyDerivation.java            # PBKDF2 key derivation
|   |   +-- VaultCrypto.java              # AES-256-GCM encryption
|   |   +-- PasswordGenerator.java        # SecureRandom passwords
|   |   +-- PasswordStrength.java         # Strength heuristic
|   +-- storage/
|   |   +-- VaultEntry.java               # Data model
|   |   +-- VaultFile.java                # Encrypted vault persistence
|   +-- ui/
|       +-- SwingUI.java                  # Desktop GUI
|       +-- ConsoleUI.java                # CLI fallback
+-- out/                   # Compiled .class files (gitignored)
+-- vault.dat              # Encrypted vault (gitignored)
+-- .gitignore
+-- README.md
```

## Known Limitations

- Not audited by security professionals.
- No master password recovery.
- Passwords exist in memory while the vault is unlocked.
- Single local vault file, no cloud sync.
- Clipboard clearing cannot protect against clipboard-monitoring malware.

## Future Improvements

- Import / export (encrypted)
- Edit existing entries
- Categories and tags
- Change master password option
- Wipe sensitive data from memory more aggressively

## Ethical Disclaimer

This project is for **educational purposes only**. It is not security-audited and is not intended for production use. **Do not store real credentials.**
