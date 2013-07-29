/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.msc._private;

import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;

/**
 * Task that sets the transactional state of a service.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
class SetTransactionalStateTask implements Executable<Void> {

    private final ServiceController<?> service;
    private final byte state;
    private final TransactionImpl transaction;

    SetTransactionalStateTask(ServiceController<?> service, byte state, TransactionImpl transaction) {
        this.service = service;
        this.state = state;
        this.transaction = transaction;
    }

    @Override
    public void execute(ExecuteContext<Void> context) {
        assert context instanceof TaskFactory;
        try {
            service.setTransition(state, transaction, (TaskFactory)context);
        } finally {
            context.complete();
        }
    }
}
