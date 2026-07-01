#include <jni.h>
#include <string>
#include <stdexcept>
#include <vector>
#include "libbbf.h"
#include "bbfcodec.h"

extern "C" {

static jclass gStringClass = nullptr;
static jmethodID gStringCtor = nullptr;
static jstring gUtf8Charset = nullptr;

static jclass gMetadataEntryClass = nullptr;
static jmethodID gMetadataEntryCtor = nullptr;

static jclass gSectionEntryClass = nullptr;
static jmethodID gSectionEntryCtor = nullptr;

static void handleCppException(JNIEnv* env, const std::exception& e) {
    jclass exClass = env->FindClass("java/lang/RuntimeException");
    if (exClass) {
        env->ThrowNew(exClass, e.what());
    }
}

static void throwIllegalArgument(JNIEnv* env, const char* message) {
    jclass exClass = env->FindClass("java/lang/IllegalArgumentException");
    if (exClass) {
        env->ThrowNew(exClass, message);
    }
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_openReader(JNIEnv* env, jclass clazz, jstring filePath) {
    if (!filePath) return 0;
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    if (!pathChars) return 0;
    
    BBFReader* reader = nullptr;
    try {
        reader = new BBFReader(pathChars);
        // Check if open was successful by checking if fileBuffer is initialized
        if (!reader->getHeaderView()) {
            delete reader;
            reader = nullptr;
        } else {
            BBFHeader* header = reader->getHeaderView();
            if (!reader->checkMagic(header)) {
                delete reader;
                reader = nullptr;
            } else {
                // Initialize footer cache so that table views can be accessed safely
                BBFFooter* footer = reader->getFooterView(header->footerOffset);
                if (!footer) {
                    delete reader;
                    reader = nullptr;
                }
            }
        }
    } catch (const std::exception& e) {
        if (reader) {
            delete reader;
            reader = nullptr;
        }
        throwIllegalArgument(env, e.what());
    }
    
    env->ReleaseStringUTFChars(filePath, pathChars);
    return reinterpret_cast<jlong>(reader);
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_openReaderFromFd(JNIEnv* env, jclass clazz, jint fd) {
    if (fd < 0) {
        throwIllegalArgument(env, "Invalid file descriptor");
        return 0;
    }
    
    BBFReader* reader = nullptr;
    try {
        reader = new BBFReader(fd);
        if (!reader->getHeaderView()) {
            delete reader;
            reader = nullptr;
        } else {
            BBFHeader* header = reader->getHeaderView();
            if (!reader->checkMagic(header)) {
                delete reader;
                reader = nullptr;
            } else {
                BBFFooter* footer = reader->getFooterView(header->footerOffset);
                if (!footer) {
                    delete reader;
                    reader = nullptr;
                }
            }
        }
    } catch (const std::exception& e) {
        if (reader) {
            delete reader;
            reader = nullptr;
        }
        handleCppException(env, e);
    }
    
    return reinterpret_cast<jlong>(reader);
}

JNIEXPORT void JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_closeReader(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (reader) {
        try {
            delete reader;
        } catch (const std::exception& e) {
            handleCppException(env, e);
        }
    }
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getPageCount(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    try {
        BBFHeader* header = reader->getHeaderView();
        if (!header) return 0;
        BBFFooter* footer = reader->getFooterView(header->footerOffset);
        if (!footer) return 0;
        return static_cast<jint>(footer->pageCount);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return 0;
    }
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetCount(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    try {
        BBFHeader* header = reader->getHeaderView();
        if (!header) return 0;
        BBFFooter* footer = reader->getFooterView(header->footerOffset);
        if (!footer) return 0;
        return static_cast<jint>(footer->assetCount);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return 0;
    }
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getSectionCount(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    try {
        BBFHeader* header = reader->getHeaderView();
        if (!header) return 0;
        BBFFooter* footer = reader->getFooterView(header->footerOffset);
        if (!footer) return 0;
        return static_cast<jint>(footer->sectionCount);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return 0;
    }
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getMetaCount(JNIEnv* env, jclass clazz, jlong readerPtr) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    try {
        BBFHeader* header = reader->getHeaderView();
        if (!header) return 0;
        BBFFooter* footer = reader->getFooterView(header->footerOffset);
        if (!footer) return 0;
        return static_cast<jint>(footer->metaCount);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return 0;
    }
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getPageAssetIndex(JNIEnv* env, jclass clazz, jlong readerPtr, jint pageIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return -1;
    try {
        BBFHeader* header = reader->getHeaderView();
        if (!header) return -1;
        BBFFooter* footer = reader->getFooterView(header->footerOffset);
        if (!footer) return -1;
        
        const uint8_t* pageTable = reader->getPageTableView(footer->pageOffset);
        if (!pageTable) return -1;
        
        const BBFPage* page = reader->getPageEntryView(pageTable, pageIndex);
        if (!page) return -1;
        
        return static_cast<jint>(page->assetIndex);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return -1;
    }
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetSize(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0;
    try {
        BBFHeader* header = reader->getHeaderView();
        if (!header) return 0;
        BBFFooter* footer = reader->getFooterView(header->footerOffset);
        if (!footer) return 0;
        
        const uint8_t* assetTable = reader->getAssetTableView(footer->assetOffset);
        if (!assetTable) return 0;
        
        const BBFAsset* asset = reader->getAssetEntryView(assetTable, assetIndex);
        if (!asset) return 0;
        
        return static_cast<jlong>(asset->fileSize);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return 0;
    }
}

JNIEXPORT jint JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetType(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return 0; // UNKNOWN
    try {
        BBFHeader* header = reader->getHeaderView();
        if (!header) return 0;
        BBFFooter* footer = reader->getFooterView(header->footerOffset);
        if (!footer) return 0;
        
        const uint8_t* assetTable = reader->getAssetTableView(footer->assetOffset);
        if (!assetTable) return 0;
        
        const BBFAsset* asset = reader->getAssetEntryView(assetTable, assetIndex);
        if (!asset) return 0;
        
        return static_cast<jint>(asset->type);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return 0;
    }
}

JNIEXPORT jobject JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetByteBuffer(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    try {
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
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return nullptr;
    }
}

JNIEXPORT jbyteArray JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetBytes(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    try {
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
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return nullptr;
    }
}

JNIEXPORT jobject JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getMetadata(JNIEnv* env, jclass clazz, jlong readerPtr, jint index) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    try {
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
        if (meta->parentOffset != 0 && meta->parentOffset != 0xFFFFFFFFFFFFFFFF) {
            parentChars = reader->getStringView(meta->parentOffset);
        }
        
        jstring keyStr = keyChars ? env->NewStringUTF(keyChars) : nullptr;
        jstring valStr = valChars ? env->NewStringUTF(valChars) : nullptr;
        jstring parentStr = parentChars ? env->NewStringUTF(parentChars) : nullptr;
        
        if (!gMetadataEntryClass || !gMetadataEntryCtor) return nullptr;
        
        return env->NewObject(gMetadataEntryClass, gMetadataEntryCtor, keyStr, valStr, parentStr);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return nullptr;
    }
}

JNIEXPORT jobject JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getSection(JNIEnv* env, jclass clazz, jlong readerPtr, jint index) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    try {
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
        if (sec->sectionParentOffset != 0 && sec->sectionParentOffset != 0xFFFFFFFFFFFFFFFF) {
            parentChars = reader->getStringView(sec->sectionParentOffset);
        }
        
        jstring titleStr = titleChars ? env->NewStringUTF(titleChars) : nullptr;
        jstring parentStr = parentChars ? env->NewStringUTF(parentChars) : nullptr;
        
        if (!gSectionEntryClass || !gSectionEntryCtor) return nullptr;
        
        return env->NewObject(gSectionEntryClass, gSectionEntryCtor, titleStr, static_cast<jlong>(sec->sectionStartIndex), parentStr);
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return nullptr;
    }
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_openBuilder(JNIEnv* env, jclass clazz, jstring outputFile, jint alignment, jint reamSize, jint flags) {
    if (!outputFile) return 0;
    const char* pathChars = env->GetStringUTFChars(outputFile, nullptr);
    if (!pathChars) return 0;
    
    BBFBuilder* builder = nullptr;
    try {
        builder = new BBFBuilder(pathChars, static_cast<uint32_t>(alignment), static_cast<uint32_t>(reamSize), static_cast<uint32_t>(flags));
    } catch (const std::exception& e) {
        throwIllegalArgument(env, e.what());
    }
    
    env->ReleaseStringUTFChars(outputFile, pathChars);
    return reinterpret_cast<jlong>(builder);
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_openBuilderFromFd(JNIEnv* env, jclass clazz, jint fd, jint alignment, jint reamSize, jint flags) {
    if (fd < 0) {
        throwIllegalArgument(env, "Invalid file descriptor");
        return 0;
    }
    
    BBFBuilder* builder = nullptr;
    try {
        builder = new BBFBuilder(fd, static_cast<uint32_t>(alignment), static_cast<uint32_t>(reamSize), static_cast<uint32_t>(flags));
    } catch (const std::exception& e) {
        throwIllegalArgument(env, e.what());
    }
    
    return reinterpret_cast<jlong>(builder);
}

JNIEXPORT void JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_closeBuilder(JNIEnv* env, jclass clazz, jlong builderPtr) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (builder) {
        try {
            delete builder;
        } catch (const std::exception& e) {
            handleCppException(env, e);
        }
    }
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addPage(JNIEnv* env, jclass clazz, jlong builderPtr, jstring filePath, jint pageFlags, jint assetFlags) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !filePath) return JNI_FALSE;
    
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    if (!pathChars) return JNI_FALSE;
    
    jboolean result = JNI_FALSE;
    try {
        result = builder->addPage(pathChars, static_cast<uint32_t>(pageFlags), static_cast<uint32_t>(assetFlags)) ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseStringUTFChars(filePath, pathChars);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addMeta(JNIEnv* env, jclass clazz, jlong builderPtr, jstring key, jstring value, jstring parent) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !key || !value) return JNI_FALSE;
    
    const char* keyChars = env->GetStringUTFChars(key, nullptr);
    const char* valChars = env->GetStringUTFChars(value, nullptr);
    const char* parentChars = parent ? env->GetStringUTFChars(parent, nullptr) : nullptr;
    
    jboolean result = JNI_FALSE;
    try {
        result = builder->addMeta(keyChars, valChars, parentChars) ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseStringUTFChars(key, keyChars);
    env->ReleaseStringUTFChars(value, valChars);
    if (parent && parentChars) {
        env->ReleaseStringUTFChars(parent, parentChars);
    }
    
    return result;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addSection(JNIEnv* env, jclass clazz, jlong builderPtr, jstring sectionName, jlong startIndex, jstring parentName) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !sectionName) return JNI_FALSE;
    
    const char* secChars = env->GetStringUTFChars(sectionName, nullptr);
    const char* parentChars = parentName ? env->GetStringUTFChars(parentName, nullptr) : nullptr;
    
    jboolean result = JNI_FALSE;
    try {
        result = builder->addSection(secChars, static_cast<uint64_t>(startIndex), parentChars) ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseStringUTFChars(sectionName, secChars);
    if (parentName && parentChars) {
        env->ReleaseStringUTFChars(parentName, parentChars);
    }
    
    return result;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_finalizeBuilder(JNIEnv* env, jclass clazz, jlong builderPtr) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder) return JNI_FALSE;
    try {
        return builder->finalize() ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return JNI_FALSE;
    }
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_petrifyFile(JNIEnv* env, jclass clazz, jstring inputPath, jstring outputPath) {
    if (!inputPath || !outputPath) return JNI_FALSE;
    
    const char* inChars = env->GetStringUTFChars(inputPath, nullptr);
    const char* outChars = env->GetStringUTFChars(outputPath, nullptr);
    
    jboolean result = JNI_FALSE;
    try {
        result = BBFBuilder::petrifyFile(inChars, outChars) ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseStringUTFChars(inputPath, inChars);
    env->ReleaseStringUTFChars(outputPath, outChars);
    
    return result;
}

JNIEXPORT jstring JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetString(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    try {
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
        
        if (!gStringClass || !gStringCtor || !gUtf8Charset) {
            env->DeleteLocalRef(array);
            return nullptr;
        }
        
        jstring result = static_cast<jstring>(env->NewObject(gStringClass, gStringCtor, array, gUtf8Charset));
        
        env->DeleteLocalRef(array);
        
        return result;
    } catch (const std::exception& e) {
        handleCppException(env, e);
        return nullptr;
    }
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_findAssetByPath(JNIEnv* env, jclass clazz, jlong readerPtr, jstring path) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader || !path) return -1;
    
    const char* pathChars = env->GetStringUTFChars(path, nullptr);
    if (!pathChars) return -1;
    
    jlong result = -1;
    try {
        result = reader->findAssetByPath(pathChars);
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseStringUTFChars(path, pathChars);
    return result;
}

JNIEXPORT jstring JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfReader_getAssetPath(JNIEnv* env, jclass clazz, jlong readerPtr, jint assetIndex) {
    BBFReader* reader = reinterpret_cast<BBFReader*>(readerPtr);
    if (!reader) return nullptr;
    
    jstring result = nullptr;
    try {
        const char* path = reader->getAssetPath(assetIndex);
        if (path) {
            result = env->NewStringUTF(path);
        }
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    return result;
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addAsset(JNIEnv* env, jclass clazz, jlong builderPtr, jstring filePath, jint assetFlags) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !filePath) return -1;
    
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    if (!pathChars) return -1;
    
    jlong result = -1;
    try {
        result = builder->addAsset(pathChars, static_cast<uint32_t>(assetFlags));
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    env->ReleaseStringUTFChars(filePath, pathChars);
    
    return result;
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addAssetWithPath(JNIEnv* env, jclass clazz, jlong builderPtr, jstring filePath, jstring logicalPath, jint assetFlags) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !filePath) return -1;
    
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    const char* logChars = logicalPath ? env->GetStringUTFChars(logicalPath, nullptr) : nullptr;
    
    jlong result = -1;
    try {
        result = builder->addAssetWithPath(pathChars, logChars, static_cast<uint32_t>(assetFlags));
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseStringUTFChars(filePath, pathChars);
    if (logicalPath && logChars) {
        env->ReleaseStringUTFChars(logicalPath, logChars);
    }
    
    return result;
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addAssetFromMemory(JNIEnv* env, jclass clazz, jlong builderPtr, jbyteArray data, jstring extensionOrName, jint assetFlags) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !data) return -1;
    
    jsize len = env->GetArrayLength(data);
    jbyte* body = env->GetByteArrayElements(data, nullptr);
    if (!body) return -1;
    
    const char* extChars = extensionOrName ? env->GetStringUTFChars(extensionOrName, nullptr) : nullptr;
    
    jlong result = -1;
    try {
        result = builder->addAssetFromMemory(reinterpret_cast<const uint8_t*>(body), static_cast<uint64_t>(len), extChars, static_cast<uint32_t>(assetFlags));
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseByteArrayElements(data, body, JNI_ABORT);
    if (extensionOrName && extChars) {
        env->ReleaseStringUTFChars(extensionOrName, extChars);
    }
    
    return result;
}

JNIEXPORT jlong JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addAssetFromMemoryWithPath(JNIEnv* env, jclass clazz, jlong builderPtr, jbyteArray data, jstring extensionOrName, jstring logicalPath, jint assetFlags) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !data) return -1;
    
    jsize len = env->GetArrayLength(data);
    jbyte* body = env->GetByteArrayElements(data, nullptr);
    if (!body) return -1;
    
    const char* extChars = extensionOrName ? env->GetStringUTFChars(extensionOrName, nullptr) : nullptr;
    const char* logChars = logicalPath ? env->GetStringUTFChars(logicalPath, nullptr) : nullptr;
    
    jlong result = -1;
    try {
        result = builder->addAssetFromMemoryWithPath(reinterpret_cast<const uint8_t*>(body), static_cast<uint64_t>(len), extChars, logChars, static_cast<uint32_t>(assetFlags));
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseByteArrayElements(data, body, JNI_ABORT);
    if (extensionOrName && extChars) {
        env->ReleaseStringUTFChars(extensionOrName, extChars);
    }
    if (logicalPath && logChars) {
        env->ReleaseStringUTFChars(logicalPath, logChars);
    }
    
    return result;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addPageByAssetIndex(JNIEnv* env, jclass clazz, jlong builderPtr, jlong assetIndex, jint pageFlags) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder) return JNI_FALSE;
    
    jboolean result = JNI_FALSE;
    try {
        result = builder->addPageByAssetIndex(static_cast<uint64_t>(assetIndex), static_cast<uint32_t>(pageFlags)) ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    return result;
}

JNIEXPORT jboolean JNICALL
Java_io_github_ahmadnaruto_libbbf_BbfBuilder_addPageWithPath(JNIEnv* env, jclass clazz, jlong builderPtr, jstring filePath, jstring logicalPath, jint pageFlags, jint assetFlags) {
    BBFBuilder* builder = reinterpret_cast<BBFBuilder*>(builderPtr);
    if (!builder || !filePath) return JNI_FALSE;
    
    const char* pathChars = env->GetStringUTFChars(filePath, nullptr);
    const char* logChars = logicalPath ? env->GetStringUTFChars(logicalPath, nullptr) : nullptr;
    
    jboolean result = JNI_FALSE;
    try {
        result = builder->addPageWithPath(pathChars, logChars, static_cast<uint32_t>(pageFlags), static_cast<uint32_t>(assetFlags)) ? JNI_TRUE : JNI_FALSE;
    } catch (const std::exception& e) {
        handleCppException(env, e);
    }
    
    env->ReleaseStringUTFChars(filePath, pathChars);
    if (logicalPath && logChars) {
        env->ReleaseStringUTFChars(logicalPath, logChars);
    }
    
    return result;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass localStringClass = env->FindClass("java/lang/String");
    if (localStringClass) {
        gStringClass = (jclass)env->NewGlobalRef(localStringClass);
        gStringCtor = env->GetMethodID(gStringClass, "<init>", "([BLjava/lang/String;)V");
        env->DeleteLocalRef(localStringClass);
    }

    jstring localUtf8 = env->NewStringUTF("UTF-8");
    if (localUtf8) {
        gUtf8Charset = (jstring)env->NewGlobalRef(localUtf8);
        env->DeleteLocalRef(localUtf8);
    }

    jclass localMetaClass = env->FindClass("io/github/ahmadnaruto/libbbf/MetadataEntry");
    if (localMetaClass) {
        gMetadataEntryClass = (jclass)env->NewGlobalRef(localMetaClass);
        gMetadataEntryCtor = env->GetMethodID(gMetadataEntryClass, "<init>",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        env->DeleteLocalRef(localMetaClass);
    }

    jclass localSecClass = env->FindClass("io/github/ahmadnaruto/libbbf/SectionEntry");
    if (localSecClass) {
        gSectionEntryClass = (jclass)env->NewGlobalRef(localSecClass);
        gSectionEntryCtor = env->GetMethodID(gSectionEntryClass, "<init>",
            "(Ljava/lang/String;JLjava/lang/String;)V");
        env->DeleteLocalRef(localSecClass);
    }

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return;
    }
    if (gStringClass) env->DeleteGlobalRef(gStringClass);
    if (gUtf8Charset) env->DeleteGlobalRef(gUtf8Charset);
    if (gMetadataEntryClass) env->DeleteGlobalRef(gMetadataEntryClass);
    if (gSectionEntryClass) env->DeleteGlobalRef(gSectionEntryClass);
}

}
