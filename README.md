# RuneBotKt
This is a discord bot with misc. functionalities to improve your discord experience, which includes, but is not limited to:
- Answering to random messages with lines from the Highschool DxD script
- A sophisticated doujin bot to fetch the numbers for you
- Several memes
- Tag system to create, manage and delete tags, which can store messages for you (For convenient meme access for example)
- Reminder/Alarm system
- Uwuifier
- Several "behaviors"

## When will I add documentation?
Yes.
## When should you add documentation?
Yes.

## Running with Docker
1. Copy `.env.example` to `.env` and fill in your bot token:
   ```
   BOT_TOKEN=your-discord-bot-token
   ```
2. Start the bot:
   ```
   docker compose up -d
   ```

The `dbs/` directory is mounted into the container so its sqlite databases persist across restarts.
