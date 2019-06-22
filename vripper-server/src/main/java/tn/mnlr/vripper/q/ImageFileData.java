package tn.mnlr.vripper.q;

import lombok.Getter;
import lombok.Setter;
import org.apache.http.client.methods.HttpUriRequest;

@Getter
@Setter
public class ImageFileData {

    private String pageUrl;
    private String imageName;
    private String imageUrl;
    private HttpUriRequest imageRequest;
}
