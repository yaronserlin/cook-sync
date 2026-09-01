package com.cooksync_server.controllers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dtos.response.ApiResponse;

/**
 * TEMPORARY - delete this class once the Render/Gmail SMTP connectivity issue is diagnosed.
 * Probes whether outbound network from this server's own runtime (unlike a local {@code nc}
 * test, which only proves reachability from the developer's machine) can reach Gmail's SMTP
 * ports. Admin-gated and limited to hardcoded hosts/ports - no caller-supplied target - so this
 * isn't an open port-scanning/SSRF primitive.
 *
 * @author Yaron Serlin
 */
@RestController
@RequestMapping("/api/admin/diagnostics")
@PreAuthorize("hasRole('ADMIN')")
public class DiagnosticsController {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    @Value("${spring.mail.host}")
    private String mailHost;

    /**
     * Attempts a raw socket connect to the configured mail host on both Gmail SMTP ports, plus
     * an HTTPS control probe, so a hang isolated to the SMTP ports (with the control succeeding)
     * points at an SMTP-specific block rather than a general outbound network problem.
     *
     * @return response entity containing per-target reachability, elapsed time, and any error
     */
    @GetMapping("/smtp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkSmtpConnectivity() {
        Map<String, Object> results = new LinkedHashMap<>();
        results.put(mailHost + ":587", probe(mailHost, 587));
        results.put(mailHost + ":465", probe(mailHost, 465));
        results.put("google.com:443 (control)", probe("google.com", 443));
        return ResponseEntity.ok(ApiResponse.success(results, "SMTP connectivity probe complete"));
    }

    private Map<String, Object> probe(String host, int port) {
        Map<String, Object> result = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            result.put("reachable", true);
        } catch (IOException e) {
            result.put("reachable", false);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        result.put("elapsedMs", System.currentTimeMillis() - start);
        return result;
    }
}
