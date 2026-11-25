package com.toolkit;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.zip.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class RenameServiceTest {

    private final RenameService service = new RenameService();

    @Test
    void renameWithReplacePrefixSuffixSeqAndDedup() throws Exception {
        var f1 = new MockMultipartFile("files", "draft_report.pdf", null, "a".getBytes());
        var f2 = new MockMultipartFile("files", "draft_summary.pdf", null, "b".getBytes());
        byte[] zip = service.rename(List.of(f1, f2), new Job("r1", "renamed.zip"),
                "2026_", "_final", "draft_", "", 1, 3);

        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) names.add(e.getName());
        }
        assertEquals(List.of("2026_report_final001.pdf", "2026_summary_final002.pdf"), names);
    }

    @Test
    void deduplicatesIdenticalOutputNames() throws Exception {
        var f1 = new MockMultipartFile("files", "doc.txt", null, "1".getBytes());
        var f2 = new MockMultipartFile("files", "doc.txt", null, "2".getBytes());
        byte[] zip = service.rename(List.of(f1, f2), new Job("r2", "renamed.zip"),
                "pre_", "", null, null, null, 3);

        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) names.add(e.getName());
        }
        assertEquals(List.of("pre_doc.txt", "pre_doc(1).txt"), names);
    }
}
