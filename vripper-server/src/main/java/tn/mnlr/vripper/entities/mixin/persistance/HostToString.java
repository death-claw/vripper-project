package tn.mnlr.vripper.entities.mixin.persistance;

import com.fasterxml.jackson.databind.util.StdConverter;
import tn.mnlr.vripper.host.Host;

public class HostToString extends StdConverter<Host, String> {

    @Override
    public String convert(Host value) {
        return value.getClass().getSimpleName();
    }
}
