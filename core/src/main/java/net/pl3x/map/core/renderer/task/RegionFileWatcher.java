package net.pl3x.map.core.renderer.task;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.log.Logger;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.util.FileUtil;
import net.pl3x.map.core.world.World;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class RegionFileWatcher implements Runnable {
    private final World world;

    private Timer timer;
    private TimerTask task;
    private Thread thread;

    private boolean stopped;

    public RegionFileWatcher(World world) {
        this.world = world;
    }

    public void start() {
        start(true);
    }

    public void start(boolean verbose) {
        if (verbose) {
            Logger.debug("Starting region file watcher for " + this.world.getName());
        }
        stop(false);
        this.timer = new Timer();
        this.task = new TimerTask() {
            @Override
            public void run() {
                RegionFileWatcher rfw = RegionFileWatcher.this;
                rfw.thread = Thread.currentThread();
                rfw.thread.setName(String.format("Pl3xMap-FileWatcher-%s", rfw.world.getName()));
                rfw.stopped = false;
                rfw.run();
            }
        };

        this.timer.schedule(task, 10000L);
    }

    public void stop() {
        stop(true);
    }

    public void stop(boolean verbose) {
        if (verbose) {
            Logger.debug("Stopping region file watcher for " + this.world.getName());
        }
        this.stopped = true;
        if (this.task != null) {
            this.task.cancel();
        }
        if (this.timer != null) {
            this.timer.cancel();
        }
        if (this.thread != null) {
            this.thread.interrupt();
        }
    }

    @Override
    public void run() {
        Path dir = this.world.getRegionDirectory();

        try (WatchService watcher = dir.getFileSystem().newWatchService()) {

            dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
            Logger.debug("Region file watcher started for " + dir);

            WatchKey key;
            while ((key = watcher.take()) != null) {
                Collection<Path> modifiedFiles = new HashSet<>();

                Logger.debug("Region file watcher got a key!");
                for (WatchEvent<?> event : key.pollEvents()) {
                    Logger.debug("Region file watcher detected event: " + event.kind().name());
                    if (event.kind() != OVERFLOW) {
                        Path file = (Path) event.context();
                        Logger.debug("Detected file change: " + file.getFileName());
                        modifiedFiles.add(dir.resolve(file));
                    }
                }
                key.reset();

                Collection<Point> points = FileUtil.regionPathsToPoints(this.world, modifiedFiles);
                Pl3xMap.api().getRegionProcessor().addRegions(this.world, points);
            }

        } catch (ClosedWatchServiceException | InterruptedException ignore) {
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (this.stopped) {
            Logger.debug("Region file watcher stopped!");
        } else {
            Logger.debug("Region file watcher stopped! Trying to start again..");
            start(false);
        }
    }
}
