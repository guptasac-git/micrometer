/*
 * Copyright 2022 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.registry.otlp;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariable;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

/**
 * Tests for {@link OtlpConfig}
 */
class OtlpConfigTest {

    @Test
    void resourceAttributesInputParsing() {
        OtlpConfig config = k -> "key1=value1,";
        assertThat(config.resourceAttributes()).containsEntry("key1", "value1").hasSize(1);
        config = k -> "k=v,a";
        assertThat(config.resourceAttributes()).containsEntry("k", "v").hasSize(1);
        config = k -> "k=v,a==";
        assertThat(config.resourceAttributes()).containsEntry("k", "v").containsEntry("a", "=").hasSize(2);
        config = k -> " k = v, a= b ";
        assertThat(config.resourceAttributes()).containsEntry("k", "v").containsEntry("a", "b").hasSize(2);
    }

    @Test
    void headersEmptyishInputParsing() {
        Stream<OtlpConfig> configs = Stream.of(k -> null, k -> "", k -> "  ", k -> " ,");
        configs.forEach(config -> assertThat(config.headers()).isEmpty());
    }

    @Test
    void headersConfigTakesPrecedenceOverEnvVars() throws Exception {
        OtlpConfig config = k -> "header1=value1";
        withEnvironmentVariable("OTEL_EXPORTER_OTLP_HEADERS", "header2=value")
                .execute(() -> assertThat(config.headers()).containsEntry("header1", "value1").hasSize(1));
    }

    @Test
    void headersUseEnvVarWhenConfigNotSet() throws Exception {
        OtlpConfig config = k -> null;
        withEnvironmentVariable("OTEL_EXPORTER_OTLP_HEADERS", "header2=value")
                .execute(() -> assertThat(config.headers()).containsEntry("header2", "value").hasSize(1));
    }

    @Test
    void combineHeadersFromEnvVars() throws Exception {
        OtlpConfig config = k -> null;
        withEnvironmentVariables().set("OTEL_EXPORTER_OTLP_HEADERS", "common=v")
                .set("OTEL_EXPORTER_OTLP_METRICS_HEADERS", "metrics=m").execute(() -> assertThat(config.headers())
                        .containsEntry("common", "v").containsEntry("metrics", "m").hasSize(2));
    }

    @Test
    void metricsHeadersEnvVarOverwritesGenericHeadersEnvVar() throws Exception {
        OtlpConfig config = k -> null;
        withEnvironmentVariables().set("OTEL_EXPORTER_OTLP_HEADERS", "metrics=m,auth=token")
                .set("OTEL_EXPORTER_OTLP_METRICS_HEADERS", "metrics=t").execute(() -> assertThat(config.headers())
                        .containsEntry("auth", "token").containsEntry("metrics", "t").hasSize(2));
    }

}
