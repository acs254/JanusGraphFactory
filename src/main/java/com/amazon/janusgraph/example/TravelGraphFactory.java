package com.amazon.janusgraph.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.amazon.janusgraph.batch.BatchCommand;
import com.amazon.janusgraph.creator.ObjectCreationCommand;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.util.stats.MetricManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

import com.amazon.janusgraph.triple.Triple;

public class TravelGraphFactory {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int BATCH_SIZE = 10;
    private static final Logger LOG = LoggerFactory.getLogger(MarvelGraphFactory.class);
    public static final String HOTEL = "name";
    public static final String BRANDTYPE = "brandtype";
    public static final String HOTELBRANDTYPE = "hotelBrandType";
    public static final String SUPERCLASS = "instanceOf";
    public static final String ISRELATED = "isRelatedTo";
    public static final String ID = "id";

    public static final MetricRegistry REGISTRY = MetricManager.INSTANCE.getRegistry();
    public static final ConsoleReporter REPORTER = ConsoleReporter.forRegistry(REGISTRY).build();
    private static final String TIMER_LINE = "TravelGraphFactory.line";
    private static final String TIMER_CREATE = "TravelGraphFactory.create_";
    private static final String COUNTER_GET = "TravelGraphFactory.get_";
    private static final AtomicInteger COMPLETED_TASK_COUNT = new AtomicInteger(0);
    private static final int POOL_SIZE = 10;

    public static void load(final JanusGraph graph, final int rowsToLoad, final boolean report) throws Exception {

        ClassLoader classLoader = TravelGraphFactory.class.getClassLoader();
        URL resource = classLoader.getResource("META-INF/HotelTriples.txt");

        Set<Triple> triples;
        triples = processFile(resource);
        BlockingQueue<Runnable> creationQueue = new LinkedBlockingQueue<>();

        for (Triple t : triples) {
            creationQueue.add(new ObjectCreationCommand(graph, t, REGISTRY, LOG));
        }

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            executor.execute(new BatchCommand(graph, creationQueue, LOG, BATCH_SIZE));
        }
        executor.shutdown();

        while (!executor.awaitTermination(60, TimeUnit.MILLISECONDS)) {
            LOG.info("Awaiting:" + creationQueue.size());
            if(report) {
                REPORTER.report();
            }
        }
        executor.shutdown();

        LOG.info("TravelGraphFactory.load complete");
    }

    public static Set<Triple> processFile(URL url) {
        Set<Triple> triples = new HashSet<>();
        try {
            BufferedReader bf = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            while ((line = bf.readLine()) != null) {
                String[] split = line.split("\t");
                Triple t = new Triple(split);
                System.out.println(String.valueOf(t));
                triples.add(t);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return triples;
    }
}
