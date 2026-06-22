#include <jni.h>
#include <string>
#include "libbbf.h"
#include "bbfcodec.h"

extern "C" {

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_openReader(JNIEnv* env, jclass clazz, jstring filePath) {
    if (!filePath) return 0;
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    if (!pathChars) return 0;
    
    BBFReader* reader = new BBFReader(pathChars);
    env->ReleaseStringUTFChars(filePath, pathChars);
    
    // Check if open was successful by checking if fileBuffer is initialized
    // (A header view would return nullptr if fileBuffer is null)
    if (!reader->getHeaderView()) {
        delete reader;
        return 0;
    }
    
    // Check magic of header
    BBFHeader* header = reader->getHeaderView();
    if (!reader->checkMagic(header)) {
        delete reader;
        return 0;
    }
    
    // Initialize footer cache so that table views can be accessed safely
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) {
        delete reader;
        return 0;
    }
    
    return reinterpret_cast<jlong>(reader);
}

JNIEXPORT void JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_closeReader(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (reader) {
        delete reader;
    }
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getPageCount(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return 0;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return 0;
    return static_cast<jint>(footer->pageCount);
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetCount(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return 0;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return 0;
    return static_cast<jint>(footer->assetCount);
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getSectionCount(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return 0;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return 0;
    return static_cast<jint>(footer->sectionCount);
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getMetaCount(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return 0;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return 0;
    return static_cast<jint>(footer->metaCount);
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getPageAssetIndex(JNIEnv* env, jclass clazz, jlong readerPtr, jint pageIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return -1;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return -1;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return -1;
    
    const uint8_t* pageTable = reader->getPageTableView(footer->pageOffset);
    if (!pageTable) return -1;
    
    const BBFPage* page = reader->getPageEntryView(pageTable, pageIndex);
    if (!page) return -1;
    
    return static_cast<jint>(page->assetIndex);
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetSize(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return 0;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return 0;
    
    const uint8_t* assetTable = reader->getAssetTableView(footer->assetOffset);
    if (!assetTable) return 0;
    
    const BBFAsset* asset = reader->getAssetEntryView(assetTable, assetIndex);
    if (!asset) return 0;
    
    return static_cast<jlong>(asset->fileSize);
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetType(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0; // UNKNOWN
    BBFHeader* header = reader->getHeaderView();
    if (!header) return 0;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return 0;
    
    const uint8_t* assetTable = reader->getAssetTableView(footer->assetOffset);
    if (!assetTable) return 0;
    
    const BBFAsset* asset = reader->getAssetEntryView(assetTable, assetIndex);
    if (!asset) return 0;
    
    return static_cast<jint>(asset->type);
}

JNIEXPORT jobject JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetByteBuffer(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return nullptr;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return nullptr;
    
    const uint8_t* assetTable = reader->getAssetTableView(footer->assetOffset);
    if (!assetTable) return nullptr;
    
    const BBFAsset* asset = reader->getAssetEntryView(assetTable, assetIndex);
    if (!asset) return nullptr;
    
    const uint8_t* data = reader->getAssetDataView(asset->fileOffset);
    if (!data) return nullptr;
    
    return env->NewDirectByteBuffer(const_cast<uint8_t*>(data), asset->fileSize);
}

JNIEXPORT jbyteArray JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetBytes(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return nullptr;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return nullptr;
    
    const uint8_t* assetTable = reader->getAssetTableView(footer->assetOffset);
    if (!assetTable) return nullptr;
    
    const BBFAsset* asset = reader->getAssetEntryView(assetTable, assetIndex);
    if (!asset) return nullptr;
    
    const uint8_t* data = reader->getAssetDataView(asset->fileOffset);
    if (!data) return nullptr;
    
    jbyteArray array = env->NewByteArray(asset->fileSize);
    if (!array) return nullptr;
    
    env->SetByteArrayRegion(array, 0, asset->fileSize, reinterpret_cast<const jbyte*>(data));
    return array;
}

JNIEXPORT jobject JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getMetadata(JNIEnv* env, jclass clazz, jlong readerPtr, jint index) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return nullptr;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return nullptr;
    
    const uint8_t* metaTable = reader->getMetadataView(footer->metaOffset);
    if (!metaTable) return nullptr;
    
    const BBFMeta* meta = reader->getMetaEntryView(metaTable, index);
    if (!meta) return nullptr;
    
    const char* keyChars = reader->getStringView(meta->keyOffset);
    const char* valChars = reader->getStringView(meta->valueOffset);
    
    const char* parentChars = nullptr;
    if (meta->parentOffset != 0) {
        parentChars = reader->getStringView(meta->parentOffset);
    }
    
    jstring keyStr = keyChars ? env->NewStringUTF(keyChars) : nullptr;
    jstring valStr = valChars ? env->NewStringUTF(valChars) : nullptr;
    jstring parentStr = parentChars ? env->NewStringUTF(parentChars) : nullptr;
    
    jclass metaClass = env->FindClass("io/github/ahmadnaruto/libbbf/MetadataEntry");
    if (!metaClass) return nullptr;
    
    jmethodID ctor = env->GetMethodID(metaClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (!ctor) return nullptr;
    
    return env->NewObject(metaClass, ctor, keyStr, valStr, parentStr);
}

JNIEXPORT jobject JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getSection(JNIEnv* env, jclass clazz, jlong readerPtr, jint index) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    BBFHeader* header = reader->getHeaderView();
    if (!header) return nullptr;
    BBFFooter* footer = reader->getFooterView(header->footerOffset);
    if (!footer) return nullptr;
    
    const uint8_t* sectionTable = reader->getSectionTableView(footer->sectionOffset);
    if (!sectionTable) return nullptr;
    
    const BBFSection* sec = reader->getSectionEntryView(sectionTable, index);
    if (!sec) return nullptr;
    
    const char* titleChars = reader->getStringView(sec->sectionTitleOffset);
    
    const char* parentChars = nullptr;
    if (sec->sectionParentOffset != 0) {
        parentChars = reader->getStringView(sec->sectionParentOffset);
    }
    
    jstring titleStr = titleChars ? env->NewStringUTF(titleChars) : nullptr;
    jstring parentStr = parentChars ? env->NewStringUTF(parentChars) : nullptr;
    
    jclass secClass = env->FindClass("io/github/ahmadnaruto/libbbf/SectionEntry");
    if (!secClass) return nullptr;
    
    jmethodID ctor = env->GetMethodID(secClass, "<init>", "(Ljava/lang/String;JLjava/lang/String;)V");
    if (!ctor) return nullptr;
    
    return env->NewObject(secClass, ctor, titleStr, static_cast<jlong>(sec->sectionStartIndex), parentStr);
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_openBuilder(JNIEnv* env, jclass clazz, jstring outputFile, jint alignment, jint reamSize, jint flags) {
    if (!outputFile) return 0;
    const char* pathChars = env->GetStringUTFChars(outputFile, nullptr);
    if (!pathChars) return 0;
    
    // Safety check: try opening path to avoid calling exit(1) inside BBFBuilder constructor
    FILE* check = fopen(pathChars, "wb");
    if (!check) {
        env->ReleaseStringUTFChars(outputFile, pathChars);
        return 0;
    }
    fclose(check);
    
    BBFBuilder* builder = new BBFBuilder(pathChars, static_cast<uint32_t>(alignment), static_cast<uint32_t>(reamSize), static_cast<uint32_t>(flags));
    env->ReleaseStringUTFChars(outputFile, pathChars);
    
    return reinterpret_cast<jlong>(builder);
}

JNIEXPORT void JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_closeBuilder(JNIEnv* env, jclass clazz, jlong builderPtr) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (builder) {
        delete builder;
    }
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addPage(JNIEnv* env, jclass clazz, jlong builderPtr, jstring filePath, jint pageFlags, jint assetFlags) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !filePath) return JNI_FALSE;
    
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    if (!pathChars) return JNI_FALSE;
    
    bool result = builder->addPage(pathChars, static_cast<uint32_t>(pageFlags), static_cast<uint32_t>(assetFlags));
    env->ReleaseStringUTFChars(filePath, pathChars);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addMeta(JNIEnv* env, jclass clazz, jlong builderPtr, jstring key, jstring value, jstring parent) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !key || !value) return JNI_FALSE;
    
    const char* keyChars = env->GetStringUTFChars(key, nullptr);
    const char* valChars = env->GetStringUTFChars(value, nullptr);
    const char* parentChars = parent ? env->GetStringUTFChars(parent, nullptr) : nullptr;
    
    bool result = builder->addMeta(keyChars, valChars, parentChars);
    
    env->ReleaseStringUTFChars(key, keyChars);
    env->ReleaseStringUTFChars(value, valChars);
    if (parent && parentChars) {
        env->ReleaseStringUTFChars(parent, parentChars);
    }
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addSection(JNIEnv* env, jclass clazz, jlong builderPtr, jstring sectionName, jlong startIndex, jstring parentName) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !sectionName) return JNI_FALSE;
    
    const char* secChars = env->GetStringUTFChars(sectionName, nullptr);
    const char* parentChars = parentName ? env->GetStringUTFChars(parentName, nullptr) : nullptr;
    
    bool result = builder->addSection(secChars, static_cast<uint64_t>(startIndex), parentChars);
    
    env->ReleaseStringUTFChars(sectionName, secChars);
    if (parentName && parentChars) {
        env->ReleaseStringUTFChars(parentName, parentChars);
    }
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_finalizeBuilder(JNIEnv* env, jclass clazz, jlong builderPtr) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder) return JNI_FALSE;
    return builder->finalize() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_petrifyFile(JNIEnv* env, jclass clazz, jstring inputPath, jstring outputPath) {
    if (!inputPath || !outputPath) return JNI_FALSE;
    
    const char* inChars = env->GetStringUTFChars(inputPath, nullptr);
    const char* outChars = env->GetStringUTFChars(outputPath, nullptr);
    
    bool result = BBFBuilder::petrifyFile(inChars, outChars);
    
    env->ReleaseStringUTFChars(inputPath, inChars);
    env->ReleaseStringUTFChars(outputPath, outChars);
    
    return result ? JNI_TRUE : JNI_FALSE;
}

}
