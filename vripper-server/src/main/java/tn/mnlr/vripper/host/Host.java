package tn.mnlr.vripper.host;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.protocol.HttpClientContext;
import org.springframework.stereotype.Service;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.services.HostService;

import java.util.Objects;

@Service
@Slf4j
abstract public class Host {

    abstract public String getHost();

    abstract public String getLookup();

    public boolean isSupported(String url) {
        return url.contains(getLookup());
    }

    public abstract HostService.NameUrl getNameAndUrl(final String url, final HttpClientContext context) throws HostException;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Host host = (Host) o;
        return Objects.equals(getHost(), host.getHost());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost());
    }

    @Override
    public String toString() {
        return getHost();
    }
}
