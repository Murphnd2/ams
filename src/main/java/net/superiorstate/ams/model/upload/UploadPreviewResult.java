package net.superiorstate.ams.model.upload;

import java.util.*;

public class UploadPreviewResult {
    public String fileName;
    public Set<String> matchedMappings = new HashSet<>();
    public boolean headerMissing = false;
    public List<String> headers = new ArrayList<>();
}
