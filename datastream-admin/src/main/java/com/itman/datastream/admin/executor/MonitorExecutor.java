/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itman.datastream.admin.executor;

import com.itman.datastream.admin.handler.MonitorHandler;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class MonitorExecutor {
    private final MonitorHandler monitorHandler;

    public void scheduledMonitor() {
        while (true) {
            try {
                monitorHandler.monitorTaskRunningQueue();

                Thread.sleep(3000);
            } catch (DataStreamException aie) {
                log.error("DataStreamException=", aie);
            } catch (InterruptedException ie) {
                log.error("Thread was interrupted", ie);
                Thread.currentThread().interrupt();
            } catch (ThreadDeath td) {
                log.error("Thread is dead", td);
            } catch (Exception e) {
                log.error("Exception=", e);
            }
        }
    }
}
