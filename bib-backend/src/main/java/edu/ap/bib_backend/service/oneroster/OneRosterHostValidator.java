package edu.ap.bib_backend.service.oneroster;

import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

class OneRosterHostValidator {
    private final List<String> allowedHostSuffixes;
    private final boolean allowPrivateHosts;

    OneRosterHostValidator(String allowedHostSuffixes, boolean allowPrivateHosts) {
        this.allowedHostSuffixes = parseSuffixes(allowedHostSuffixes);
        this.allowPrivateHosts = allowPrivateHosts;
    }

    String normalizeAndValidateSchoolHost(String schoolCode) {
        String candidate = normalizeSchoolCodeToHost(schoolCode);
        validateHost(candidate);
        return candidate;
    }

    /**
     * @param schoolCode raw school identifier (URL or plain host)
     * @return extracted lowercase hostname
     */
    private String normalizeSchoolCodeToHost(String schoolCode) {
        if (schoolCode == null || schoolCode.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Geen geldige schoolcontext gevonden");
        }

        String trimmed = schoolCode.trim();
        try {
            URI uri = URI.create(trimmed);
            if (uri.getHost() != null) {
                return uri.getHost().toLowerCase(Locale.ROOT);
            }
        } catch (IllegalArgumentException ignored) {
            // When schoolCode is a plain host, URI parsing can fail without a scheme.
        }

        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("@") || trimmed.contains(":")) {
            throw new ResponseStatusException(BAD_REQUEST, "School host is ongeldig geformatteerd");
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * @param host the hostname to validate
     */
    private void validateHost(String host) {
        if (!host.matches("^[a-z0-9.-]+$")) {
            throw new ResponseStatusException(BAD_REQUEST, "School host bevat ongeldige tekens");
        }

        if (host.startsWith(".") || host.endsWith(".") || host.contains("..")) {
            throw new ResponseStatusException(BAD_REQUEST, "School host is ongeldig");
        }

        if (!allowPrivateHosts && (host.equals("localhost") || host.endsWith(".localhost") || isIpLiteral(host))) {
            throw new ResponseStatusException(BAD_REQUEST, "School host is niet toegestaan");
        }

        boolean matchesAllowedSuffix = allowedHostSuffixes.stream()
                .anyMatch(host::endsWith);
        if (!matchesAllowedSuffix) {
            throw new ResponseStatusException(BAD_REQUEST, "School host valt buiten toegestane domeinen");
        }

        try {
            InetAddress[] resolved = InetAddress.getAllByName(host);
            if (!allowPrivateHosts) {
                for (InetAddress address : resolved) {
                    if (address.isAnyLocalAddress()
                            || address.isLoopbackAddress()
                            || address.isLinkLocalAddress()
                            || address.isSiteLocalAddress()
                            || address.isMulticastAddress()) {
                        throw new ResponseStatusException(BAD_REQUEST, "School host resolveert naar een privaat adres");
                    }
                }
            }
        } catch (UnknownHostException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "School host kon niet gevalideerd worden", ex);
        }
    }

    private boolean isIpLiteral(String host) {
        if (host.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
            return true;
        }
        return host.contains(":");
    }

    private List<String> parseSuffixes(String rawSuffixes) {
        String suffixesSource = (rawSuffixes == null || rawSuffixes.isBlank()) ? ".smartschool.be" : rawSuffixes;
        return java.util.Arrays.stream(suffixesSource.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.startsWith(".") ? value.toLowerCase(Locale.ROOT)
                        : "." + value.toLowerCase(Locale.ROOT))
                .toList();
    }
}
