package org.flowant.stats.util;

import static org.flowant.stats.Config.SEP_DIR;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.flowant.stats.BaseTest;
import org.flowant.util.FileUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FileUtilTest extends BaseTest {
    String filePath = null;

    @Before
    public void setUp() {
        filePath = "test_" + uniqueId + ".xml";
    }

    @After
    public void tearDown() throws IOException {

    }

    @Test
    public void writeReadStringToFile() throws IOException {
        FileUtil.rmdirs(filePath);
        String contents = this.getClass().getCanonicalName();
        FileUtil.writeStringToFile(filePath, contents);
        String readContents = FileUtil.readStringFromLittleFile(filePath);
        Assert.assertEquals(readContents, contents);
        FileUtil.rmdirs(filePath);
    }

    @Test
    public void listFiles() throws IOException {
        String sourceDir = testSourcePath + SEP_DIR + this.getClass().getPackage().getName().replace(".", SEP_DIR);
        List<String> fileList = FileUtil.listFiles(sourceDir);
        String sourcePath = sourceDir + SEP_DIR + this.getClass().getSimpleName() + ".java";
        Assert.assertTrue(fileList.contains(sourcePath));
    }

    @Test
    public void toSafePath() throws IOException {
        String orgPath = "\"/?\\*<>|EARTH, expDlr, skip:0, limit:10, 201507 ~ 201704.jpg";
        String encodedPath = FileUtil.toSafePath(orgPath);
        logger.log(l, () -> "orgPath:" + orgPath);
        logger.log(l, () -> "encodedPath:" + encodedPath);
        Path path = Paths.get(encodedPath);
        Files.createFile(path);
        Assert.assertTrue(Files.exists(path));
        FileUtil.rmdirs(path);
    }

}
