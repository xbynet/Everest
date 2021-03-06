/*
 * Copyright 2018 Rohit Awate.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rohitawate.everest.misc;

import com.google.common.util.concurrent.MoreExecutors;
import com.rohitawate.everest.controllers.HomeWindowController;
import com.rohitawate.everest.history.HistoryManager;
import com.rohitawate.everest.logging.Level;
import com.rohitawate.everest.logging.LoggingService;
import com.rohitawate.everest.requestmanager.RequestManagersPool;

import java.util.concurrent.Executor;

public class Services {
    public static Thread startServicesThread;
    public static HistoryManager historyManager;
    public static LoggingService loggingService;
    public static HomeWindowController homeWindowController;
    public static Executor singleExecutor;
    public static RequestManagersPool pool;

    public static void start() {
        startServicesThread = new Thread(() -> {
            loggingService = new LoggingService(Level.INFO);
            historyManager = new HistoryManager();
            singleExecutor = MoreExecutors.directExecutor();
            pool = new RequestManagersPool();
        });

        startServicesThread.start();
    }
}
