package tn.mnlr.vripper.entities.mixin.persistance;

import com.fasterxml.jackson.databind.util.StdConverter;
import tn.mnlr.vripper.SpringContext;
import tn.mnlr.vripper.host.Host;

import java.util.Collection;

public class StringToHost extends StdConverter<String, Host> {

    private Collection<Host> hosts = SpringContext.getBeansOfType(Host.class).values();

    @Override
    public Host convert(String value) {
        return hosts.stream().filter(e -> e.getClass().getSimpleName().equals(value)).findAny().orElse(null);
    }
}
