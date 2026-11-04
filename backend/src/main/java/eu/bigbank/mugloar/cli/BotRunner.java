package eu.bigbank.mugloar.cli;

import eu.bigbank.mugloar.client.GameApi;
import eu.bigbank.mugloar.config.MugloarProperties;
import eu.bigbank.mugloar.domain.GameState;
import eu.bigbank.mugloar.strategy.AdEvaluator;
import eu.bigbank.mugloar.strategy.BotEvent;
import eu.bigbank.mugloar.strategy.GameBot;
import eu.bigbank.mugloar.strategy.ShopPolicy;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Usage: {@code ./gradlew bootRun --args='--spring.profiles.active=bot --bot.games=10'} */
@Component
@Profile("bot")
public class BotRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BotRunner.class);
    static final int TARGET_SCORE = 1000;

    private final GameBot bot;
    private final int games;

    public BotRunner(GameApi api, AdEvaluator evaluator, ShopPolicy shopPolicy, MugloarProperties props,
            @Value("${bot.games:5}") int games) {
        this.bot = new GameBot(api, evaluator, shopPolicy, props.bot().maxTurns());
        this.games = Math.max(1, games);
    }

    private static void logEvent(BotEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("{}", event);
        }
    }

    @Override
    public void run(String... args) {
        List<Integer> scores = new ArrayList<>();
        for (int i = 1; i <= games; i++) {
            GameState end = bot.playNewGame(BotRunner::logEvent, () -> false);
            scores.add(end.score());
            log.info("Game {}/{} ({}): score {} after {} turns, level {}",
                    i, games, end.gameId(), end.score(), end.turn(), end.level());
        }
        IntSummaryStatistics stats = scores.stream().mapToInt(Integer::intValue).summaryStatistics();
        long reached = scores.stream().filter(s -> s >= TARGET_SCORE).count();
        log.info("Done. min {} / avg {} / max {}; {} of {} games reached {}+",
                stats.getMin(), Math.round(stats.getAverage()), stats.getMax(), reached, games, TARGET_SCORE);
    }
}
