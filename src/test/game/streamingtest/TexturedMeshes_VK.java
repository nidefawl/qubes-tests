package test.game.streamingtest;


import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

import java.io.*;
import java.nio.*;
import java.util.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription.Buffer;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.assets.*;
import nidefawl.qubes.assets.AssetTexture.Type;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.input.CameraController;
import nidefawl.qubes.render.post.SMAA;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.texture.DDSLoader;
import nidefawl.qubes.texture.DDSLoader.Format;
import nidefawl.qubes.texture.TextureBinMips;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.vulkan.*;
import nidefawl.qubes.vulkan.VkMemoryManager.MemoryChunk;

public class TexturedMeshes_VK extends GameBase {
	static {
		System.setProperty("renderer.vulkan", "true");
	}
	static final boolean INVERSE_Z = true;
	public TexturedMeshes_VK() {
		TICKS_PER_SEC = 20;
		DEBUG_LAYER = true;
	}
	public static void main(String[] args) {

        GameContext.setSideAndPath(Side.CLIENT, "../Game/");
		GameContext.earlyInit();
		new TexturedMeshes_VK().startGame();
	}

    final CameraController cameraController = new CameraController();
	private boolean update = true;
	private boolean updateModeChanged;

	@Override
	public void onStatsUpdated() {
		String stats = lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+") bytes uploaded: "+Stats.uploadBytes;
		setTitle(stats);
		AssetManager assetManager = AssetManagerClient.getInstance();
		VkShader n = vkContext.loadCompileGLSL(assetManager, "shaders/textured.fsh", VK_SHADER_STAGE_FRAGMENT_BIT);
		VkShader n2 = vkContext.loadCompileGLSL(assetManager, "shaders/textured.vsh", VK_SHADER_STAGE_VERTEX_BIT);
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {

		if (action == GLFW.GLFW_PRESS) {
			switch (key) {
			case GLFW.GLFW_KEY_SPACE:
				update = !update;
				updateModeChanged = true;
				Arrays.fill(recorded, false);
				break;
			}
		}

	}

	int bufferSize = 12312;
	Random rand = new Random();
	@Override
	public void render(float f) {
		int currentBuffer = VKContext.currentBuffer;
		VkCommandBuffer buffer = renderCommandBuffers[currentBuffer];
		if (!recorded[currentBuffer]||update) {
			recorded[currentBuffer]=true;
			vkResetCommandBuffer(buffer, 0);
	    	long fbSwapchain = vkContext.swapChain.framebuffers[currentBuffer];
	        createRenderCommandBuffers(buffer, fbSwapchain);
		}
		this.vkContext.submitCommandBuffer(buffer);
	}
	
	int bufferoffset = 0;
	@Override
	public void preRenderUpdate(float f) {
		this.cameraController.orientCamera(Engine.camera, movement, VR_SUPPORT, f);
        Engine.updateCamera();
		if (needsRebuildPipeLine) {
			needsRebuildPipeLine = false;
			rebuildPipelines();
		}
		if (updateModeChanged) {
			vkContext.syncAllFences();
		}
        UniformBuffer.updateUBO(null, f);
        if (update) {
        	drawScene(VKContext.currentBuffer, false, rand.nextInt(12)*4);
        } else if (this.cube[0]==null||this.cube[0].idxCount==0||updateModeChanged) {
//        	for (int i = 0; i < vkContext.swapChain.numImages; i++) {
        		drawScene(0, true, 0);
//        	}
        } else {
        }
        updateModeChanged = false;
	}

	private void drawScene(int idx, boolean makeDeviceLocal, int offset) {
		bufferoffset = offset;
		VkTess tess = VkTess.instance;
		int l = 12;
		float d = 5;
		for (int x = -l; x <= l; x++) {
			for (int z = -l; z <= l; z++) {
				for (int y = -l; y <= l; y++) {
					tess.setOffset(x*d, y*d, z*d);
					tess.setNormals(0, 0, 1);
					tess.add( 1,  1, 1, 1, 1);
					tess.add(-1,  1, 1, 0, 1);
					tess.add(-1, -1, 1, 0, 0);
					tess.add( 1, -1, 1, 1, 0);
					tess.setNormals(0, 0, -1);
					tess.add(-1, -1, -1, 0, 0);
					tess.add(-1,  1, -1, 0, 1);
					tess.add( 1,  1, -1, 1, 1);
					tess.add( 1, -1, -1, 1, 0);
					tess.setNormals(0, 1, 0);
					tess.add(-1,  1, -1, 0, 0);
					tess.add(-1,  1,  1, 0, 1);
					tess.add( 1,  1,  1, 1, 1);
					tess.add( 1,  1, -1, 1, 0);
					tess.setNormals(0, -1, 0);
					tess.add(-1, -1,  1, 0, 1);
					tess.add(-1, -1, -1, 0, 0);
					tess.add( 1, -1, -1, 1, 0);
					tess.add( 1, -1,  1, 1, 1);
					tess.setNormals(1, 0, 0);
					tess.add( 1, -1, -1, 0, 1);
					tess.add( 1, -1,  1, 0, 0);
					tess.add( 1,  1,  1, 1, 0);
					tess.add( 1,  1, -1, 1, 1);
					tess.setNormals(-1, 0, 0);
					tess.add(-1, -1, -1, 0, 1);
					tess.add(-1, -1,  1, 0, 0);
					tess.add(-1,  1,  1, 1, 0);
					tess.add(-1,  1, -1, 1, 1);
				}
				
			}
		}
		int bufferMode = makeDeviceLocal ? VkTess.DEVICE_LOCAL_UPLOAD : VkTess.STREAM_UPLOAD;
		tess.finish(VkTess.CREATE_QUAD_IDX_BUFFER, bufferMode, cube[idx], bufferoffset);
		tess.setOffset(0, 0, 0);
		tess.setNormals(0, 0, 1);
		tess.add( 2,  2, 3.5f, 1, 1);
		tess.add(-2,  2, 3.5f, 0, 1);
		tess.add(-2, -2, 3.5f, 0, 0);
		tess.add( 2, -2, 3.5f, 1, 0);
		tess.finish(VkTess.CREATE_QUAD_IDX_BUFFER, bufferMode, plane[idx], bufferoffset);
    
	}
	@Override
	public void postRenderUpdate(float f) {
	}

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
        if (isRunning()) {
            Engine.resize(displayWidth, displayHeight);
        }
	}

	@Override
	public void initGame() {
		Engine.init(EngineInitSettings.INIT_NONE.setVulkan(true).setInverseZ());
		setVSync(false);
		camera = new Camera();
		
		AssetBinary bin = AssetManagerClient.getInstance().loadBin("vulkan/texture.bin");
		texture2dData = new TextureBinMips(bin);
		this.cameraController.set(-1.58f, 0.01f, 1.50f, 0.00f, 17.64f);
	}
	static class Texture {
		int width;
		int height;
		int mipLevels;
		int textureLayout;
		public long image;
		public int imageLayout;
		private long view;
	}
	TextureBinMips texture2dData;
	boolean needsRebuildPipeLine = true;
    public Camera         camera;
	Texture texture = new Texture();
	private VkCommandBuffer[] renderCommandBuffers;
	private long[] descriptorSets;
	private AssetTexture t;
	private long sampler;
	private long descriptorSetLayout;
	private long descriptorPool = VK_NULL_HANDLE;

	VkTesselatorState[] cube;
	VkTesselatorState[] plane;
	@Override
	public void lateInitGame() {

		loadTexture(this.texture2dData);
		setupDescriptorSetLayout();
	}
	private void setupDescriptorPool(int numImages) {
        try ( MemoryStack stack = stackPush() ) {
        	
    		VkDescriptorPoolSize poolSize0 = VkInitializers.descriptorPoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, numImages);
    		VkDescriptorPoolSize poolSize1 = VkInitializers.descriptorPoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, numImages);
    		VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(2, stack);
    		poolSizes.put(0, poolSize0);
    		poolSizes.put(1, poolSize1);
    		
    		VkDescriptorPoolCreateInfo descriptorPoolInfo = VkInitializers.descriptorPoolCreateInfo(poolSizes, 2);
    		descriptorPoolInfo.maxSets(numImages);
			LongBuffer pDescriptorPool = stack.longs(0);
			int err = vkCreateDescriptorPool(vkContext.device, descriptorPoolInfo, null, pDescriptorPool);
	        if (err != VK_SUCCESS) {
	            throw new AssertionError("vkCreateDescriptorPool failed: " + VulkanErr.toString(err));
	        }
	        this.descriptorPool = pDescriptorPool.get(0);
        }
	}
	private void setupDescriptorSetLayout() {
        try ( MemoryStack stack = stackPush() ) {
    		VkDescriptorSetLayoutBinding.Buffer setLayoutBindings = VkDescriptorSetLayoutBinding.callocStack(2, stack);
    		setLayoutBindings.get(0)
    			.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
	    		.stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
	    		.binding(0)
	    		.descriptorCount(1);
    		setLayoutBindings.get(1)
    			.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
    			.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
    			.binding(1)
    			.descriptorCount(1);
    		VkDescriptorSetLayoutCreateInfo descriptorLayoutCI = VkInitializers.descriptorSetLayoutCreateInfo(setLayoutBindings);
    		LongBuffer pDescriptorSetLayout = stack.longs(0);
    		int err = vkCreateDescriptorSetLayout(vkContext.device, descriptorLayoutCI, null, pDescriptorSetLayout);
	        if (err != VK_SUCCESS) {
	            throw new AssertionError("vkCreateDescriptorSetLayout failed: " + VulkanErr.toString(err));
	        }
	        this.descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
	}
	private void setupDescriptorSets(int numImages) {
		if (this.descriptorPool != VK_NULL_HANDLE) {
	    	vkDestroyDescriptorPool(vkContext.device, descriptorPool, null);
		}
		setupDescriptorPool(numImages);
        try ( MemoryStack stack = stackPush() ) {
        	LongBuffer pDescriptorSetLayouts = stack.callocLong(numImages);
        	for (int i = 0; i < numImages; i++) {
        		pDescriptorSetLayouts.put(i, descriptorSetLayout);
        	}
        	VkDescriptorSetAllocateInfo allocInfo = VkInitializers.descriptorSetAllocateInfo(this.descriptorPool, pDescriptorSetLayouts);
			LongBuffer pDescriptorSet = stack.callocLong(numImages);
        	int err = vkAllocateDescriptorSets(vkContext.device, allocInfo, pDescriptorSet);
        	this.descriptorSets = new long[numImages];
        	pDescriptorSet.get(this.descriptorSets);
	        if (err != VK_SUCCESS) {
	            throw new AssertionError("vkAllocateDescriptorSets failed: " + VulkanErr.toString(err));
	        }
	        VkDescriptorImageInfo.Buffer textureDescriptor = VkDescriptorImageInfo.callocStack(1, stack);
	        textureDescriptor.imageView(texture.view);
	        textureDescriptor.sampler(sampler);
	        textureDescriptor.imageLayout(texture.imageLayout);
	        VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.callocStack(2, stack);
	        for (int i = 0; i < numImages; i++) {
		        VkInitializers.writeDescriptorSet(writeDescriptorSet, 1, this.descriptorSets[i], VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1, textureDescriptor);
		        VkInitializers.writeDescriptorSet(writeDescriptorSet, 0, this.descriptorSets[i], VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 0, UniformBuffer.uboMatrix3D.getDescriptorBuffer(i));
		        vkUpdateDescriptorSets(vkContext.device, writeDescriptorSet, null);
	        }
        }
	}
	VkShader vert;
//	VkShader vertTest;
	VkShader frag;
	void rebuildPipelines()
	{
//		if (vertTest != null) vertTest.destroy();
		if (vert != null) vert.destroy();
		if (frag != null) frag.destroy();
		AssetManager assetManager = AssetManagerClient.getInstance();
		vert = vkContext.loadCompileGLSL(assetManager, "vulkan/shaders/texture.vert", VK_SHADER_STAGE_VERTEX_BIT);
		frag = vkContext.loadShader(assetManager, "vulkan/shaders/texture.frag.spv", VK_SHADER_STAGE_FRAGMENT_BIT);
        try ( MemoryStack stack = stackPush() ) {
            VkPipelines.main.setDescriptorSetLayout(this.descriptorSetLayout);
            VkPipelines.main.setRenderPass(vkContext.renderPass);
            VkPipelines.main.setShaders(vert, frag);
            VkPipelines.main.setVertexDesc(getVertexDesc());
            VkPipelines.buildPipeLine(vkContext, VkPipelines.main);
        }
	}
	static VkVertexDescriptors getVertexDesc() {
		return GLVAO.vaoTesselator[1|2].getVkVertexDesc();
	}

	private void loadTexture(TextureBinMips texture2dData) {
		int vkFormat = VK_FORMAT_BC2_UNORM_BLOCK;

        try ( MemoryStack stack = stackPush() ) {
        	
        VkFormatProperties formatProperties = VkFormatProperties.callocStack(stack);
        vkGetPhysicalDeviceFormatProperties(vkContext.getPhysicalDevice(), vkFormat, formatProperties);
		boolean useStaging = true;
		boolean forceLinearTiling = false;
		int totalSize = texture2dData.totalSize;
		texture.width = texture2dData.w[0];
		texture.height = texture2dData.h[0];
		texture.mipLevels = texture2dData.mips;
		
		ByteBuffer dataDirect = ByteBuffer.allocateDirect(totalSize).order(ByteOrder.nativeOrder());
		dataDirect.clear();
		dataDirect.put(texture2dData.data);
		dataDirect.flip();
//		for (int a = 0; a < texture2dData.data.length; a++) {
//			System.out.printf("0x%02X, ",texture2dData.data[a]);
//			if (a%16==0)
//				System.out.println();
//			if (a > 30)
//				break;
//		}
		// Only use linear tiling if forced
		if (forceLinearTiling)
		{
			// Don't use linear if format is not supported for (linear) shader sampling
			useStaging = (formatProperties.linearTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_BIT) == 0;
		}


			if (useStaging)
			{
				LongBuffer stagingBuffer = stack.longs(0);
				LongBuffer pImage = stack.longs(0);
				VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.callocStack(stack)
		                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
		                .usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
		                .size(totalSize);
				bufferCreateInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
				int err = vkCreateBuffer(vkContext.device, bufferCreateInfo, null, stagingBuffer);
				if (err != VK_SUCCESS) {
					throw new AssertionError("vkCreateBuffer failed: " + VulkanErr.toString(err));
				}

				MemoryChunk memChunk = vkContext.memoryManager.allocateBufferMemory(stagingBuffer.get(0), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
				long l = memChunk.map();
	            System.out.println("copy from "+memAddress(dataDirect)+" to "+l+", size="+totalSize);
		        memCopy(memAddress(dataDirect), l, totalSize);
		        memChunk.unmap();
		        
		        int offset = 0;
		        
		        VkBufferImageCopy.Buffer bufferCopyRegions = VkBufferImageCopy.callocStack(texture.mipLevels, stack);
				for (int i = 0; i < texture.mipLevels; i++)
				{
					VkBufferImageCopy bufferCopyRegion = bufferCopyRegions.get(i);
					VkImageSubresourceLayers bufferCopyRegionSubresource = bufferCopyRegion.imageSubresource();
					bufferCopyRegionSubresource.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
					bufferCopyRegionSubresource.mipLevel(i);
					bufferCopyRegionSubresource.baseArrayLayer(0);
					bufferCopyRegionSubresource.layerCount(1);
					VkExtent3D imageExtend = bufferCopyRegion.imageExtent();
					imageExtend.width(texture2dData.w[i]).height(texture2dData.h[i]).depth(1);
					bufferCopyRegion.bufferOffset(offset);
					offset += texture2dData.sizes[i];
				}
	
				// Create optimal tiled target image
				VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.callocStack(stack)
						.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
						.imageType(VK_IMAGE_TYPE_2D)
						.format(vkFormat)
						.mipLevels(texture.mipLevels)
						.arrayLayers(1)
						.samples(VK_SAMPLE_COUNT_1_BIT)
						.tiling(VK_IMAGE_TILING_OPTIMAL)
						.usage(VK_IMAGE_USAGE_SAMPLED_BIT)
						.sharingMode(VK_SHARING_MODE_EXCLUSIVE)
						// Set initial layout of the image to undefined;
						.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
						.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT);
				imageCreateInfo.extent().width(texture.width).height(texture.height).depth(1);
				err = vkCreateImage(vkContext.device, imageCreateInfo, null, pImage);
		        if (err != VK_SUCCESS) {
		            throw new AssertionError("vkCreateImage failed: " + VulkanErr.toString(err));
		        }
		        vkContext.memoryManager.allocateImageMemory(pImage.get(0), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, VkConstants.TEXTURE_COLOR_MEMORY);
		        VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.callocStack(stack)
		                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
		                .commandPool(vkContext.renderCommandPool)
		                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
		                .commandBufferCount(1);
		        PointerBuffer pCommandBuffer = stack.pointers(0);
		        err = vkAllocateCommandBuffers(vkContext.device, cmdBufAllocateInfo, pCommandBuffer);
		        if (err != VK_SUCCESS) {
		            throw new AssertionError("Failed to allocate command buffer: " + VulkanErr.toString(err));
		        }
		        VkCommandBuffer copyCmd = new VkCommandBuffer(pCommandBuffer.get(0), vkContext.device);
		        // Create the command buffer begin structure
		        VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.callocStack(stack)
		                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
		                .pNext(NULL);
		        vkBeginCommandBuffer(copyCmd, cmdBufInfo);
	
				// Image barrier for optimal image
		        
				// The sub resource range describes the regions of the image we will be transition
		        VkImageSubresourceRange subresourceRange = VkImageSubresourceRange.callocStack(stack)
		    			// Image only contains color data
		        		.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
		    			// Start at first mip level
		        		.baseMipLevel(0)
		    			// We will transition on all mip levels
		        		.levelCount(texture.mipLevels)
		    			// The 2D texture only has one layer
		        		.layerCount(1);
				// Optimal image will be used as destination for the copy, so we must transfer from our
				// initial undefined image layout to the transfer destination layout
		        setImageLayout(copyCmd, 
		        		pImage.get(0), 
		        		VK_IMAGE_ASPECT_COLOR_BIT, 
		        		VK_IMAGE_LAYOUT_UNDEFINED, 
		        		VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 
		        		subresourceRange);
				// Clean up staging resources
	
				// Copy mip levels from staging buffer
				vkCmdCopyBufferToImage(
					copyCmd,
					stagingBuffer.get(0),
	        		pImage.get(0), 
					VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
					bufferCopyRegions);
	
				// Change texture image layout to shader read after all mip levels have been copied
				setImageLayout(
					copyCmd,
					pImage.get(0),
					VK_IMAGE_ASPECT_COLOR_BIT,
					VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
					VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
					subresourceRange);
				texture.image = pImage.get(0);
				texture.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
				vkEndCommandBuffer(copyCmd);
		        VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
		        
		        PointerBuffer pCommandBuffers = stack.pointers(copyCmd);
		        submitInfo.pCommandBuffers(pCommandBuffers);
		        err = vkQueueSubmit(vkContext.vkQueue, submitInfo, VK_NULL_HANDLE);
		        if (err != VK_SUCCESS) {
		            throw new AssertionError("vkQueueSubmit failed: " + VulkanErr.toString(err));
		        }
		        err = vkQueueWaitIdle(vkContext.vkQueue);
		        if (err != VK_SUCCESS) {
		            throw new AssertionError("vkQueueWaitIdle failed: " + VulkanErr.toString(err));
		        }
				vkFreeCommandBuffers(vkContext.device, vkContext.renderCommandPool, pCommandBuffers);
				System.out.println("release staging buffer memory");
				vkContext.memoryManager.releaseBufferMemory(stagingBuffer.get(0));
				vkDestroyBuffer(vkContext.device, stagingBuffer.get(0), null);
			}
			
			VkSamplerCreateInfo sampler = VkSamplerCreateInfo.callocStack(stack)
					.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
					.magFilter(VK_FILTER_LINEAR)
					.minFilter(VK_FILTER_LINEAR)
					.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
					.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
					.mipLodBias(0.0f)
					.compareOp(VK_COMPARE_OP_NEVER)
					.minLod(0.0f);
			// Set max level-of-detail to mip level count of the texture
			sampler.maxLod((useStaging) ? (float)texture.mipLevels : 0.0f);
			// Enable anisotropic filtering
			// This feature is optional, so we must check if it's supported on the device
			if (vkContext.features.samplerAnisotropy())
			{
				sampler.maxAnisotropy(vkContext.limits.maxSamplerAnisotropy());
				sampler.anisotropyEnable(true);
			} else {
				sampler.maxAnisotropy(1.0f);
				sampler.anisotropyEnable(false);
			}
			sampler.borderColor(VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE);
	        LongBuffer pSampler = stack.longs(0);
	        int err = vkCreateSampler(vkContext.device, sampler, null, pSampler);
	        if (err != VK_SUCCESS) {
	            throw new AssertionError("vkCreateSampler failed: " + VulkanErr.toString(err));
	        }
	        this.sampler = pSampler.get(0);
	        
	        VkImageViewCreateInfo view = VkImageViewCreateInfo.callocStack(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
	        		.viewType(VK_IMAGE_VIEW_TYPE_2D)
	        		.format(vkFormat)
	        		.components(VkComponentMapping.callocStack(stack));
	        VkImageSubresourceRange viewSubResRange = view.subresourceRange();
	        viewSubResRange.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
	        viewSubResRange.baseMipLevel(0);
	        viewSubResRange.baseArrayLayer(0);
	        viewSubResRange.layerCount(1);
			// Linear tiling usually won't support mip maps
			// Only set mip map count if optimal tiling is used
	        viewSubResRange.levelCount((useStaging) ? texture.mipLevels : 1);
			// The view will be based on the texture's image
	        view.image(texture.image);
	        LongBuffer pView = stack.longs(0);
	        err = vkCreateImageView(vkContext.device, view, null, pView);
	        if (err != VK_SUCCESS) {
	            throw new AssertionError("vkCreateImageView failed: " + VulkanErr.toString(err));
	        }
	        this.texture.view = pView.get(0);
		}
	}

	
	private void setImageLayout(VkCommandBuffer cmdBuffer, long l, int aspectMask,
			int oldLayout, int newLayout, VkImageSubresourceRange subresourceRange) {
		VkImageMemoryBarrier.Buffer imageMemoryBarrier = VkImageMemoryBarrier.callocStack(1).sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
				.oldLayout(oldLayout)
				.newLayout(newLayout)
				.image(l).subresourceRange(subresourceRange);

		// Only sets masks for layouts used in this example
		// For a more complete version that can be used with other layouts see vkTools::setImageLayout

		// Source layouts (old)
		switch (oldLayout)
		{
		case VK_IMAGE_LAYOUT_UNDEFINED:
			// Only valid as initial layout, memory contents are not preserved
			// Can be accessed directly, no source dependency required
			imageMemoryBarrier.srcAccessMask(0);
			break;
		case VK_IMAGE_LAYOUT_PREINITIALIZED:
			// Only valid as initial layout for linear images, preserves memory contents
			// Make sure host writes to the image have been finished
			imageMemoryBarrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT);
			break;
		case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:
			// Old layout is transfer destination
			// Make sure any writes to the image have been finished
			imageMemoryBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			break;
		}

		// Target layouts (new)
		switch (newLayout)
		{
		case VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL:
			// Transfer source (copy, blit)
			// Make sure any reads from the image have been finished
			imageMemoryBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
			break;
		case VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL:
			// Transfer destination (copy, blit)
			// Make sure any writes to the image have been finished
			imageMemoryBarrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			break;
		case VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL:
			// Shader read (sampler, input attachment)
			imageMemoryBarrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
			break;
		}
		// Put barrier on top of pipeline
		int srcStageFlags = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
		int destStageFlags = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;

		// Put barrier inside setup command buffer
		vkCmdPipelineBarrier(
			cmdBuffer,
			srcStageFlags, 
			destStageFlags, 
			0, 
			null,
			null,
			imageMemoryBarrier);
	}

	float lastExtend = 1.0f;
	float extend = 1.0f;
	private VkBuffer tmpBuffer1;
	private VkBuffer tmpBuffer2;
	int nTick = 0;
	@Override
	public void tick() {
		if (!isStarting) {
			this.cameraController.tickUpdate();
//			if (nTick++%40==0) {
//				if (this.rand.nextInt(4)==0) {
//					if (tmpBuffer1 != null) {
//						tmpBuffer1.destroy();
//					}
//					bufferSize = rand.nextInt(100000)+555;
//					tmpBuffer1 = vkContext.createBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
//							bufferSize, false, "dummy1");
//				}
//				if (this.rand.nextInt(4)==0) {
//					if (tmpBuffer2 != null) {
//						tmpBuffer2.destroy();
//					}
//					bufferSize = rand.nextInt(100000)+555;
//					tmpBuffer2 = vkContext.createBuffer(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
//							bufferSize, false, "dummy2");
//				}
//			}
		}
		lastExtend = extend;
		extend = 1.0f+GameMath.sin(ticksran*1.03f)*0.5f;
	}
	
	InitCrap initCrap;
	private boolean[] recorded;
	static class InitCrap {
		LongBuffer pDescriptorSets = memAllocLong(1);
		LongBuffer pOffsets = memAllocLong(1);
        VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pNext(NULL).flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT);
        VkClearValue.Buffer clearValues = VkClearValue.calloc(2);

        VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .pNext(NULL)
                .renderPass(0)
                .pClearValues(clearValues);
		private VkExtent2D renderAreaExtent;
		private VkOffset2D renderAreaOffset;
        public InitCrap() {
            clearValues.get(0).color()
            .float32(0, 100/255.0f)
            .float32(1, 100/255.0f)
            .float32(2, 100/255.0f)
            .float32(3, 1.0f);
        	clearValues.get(1).depthStencil().set(INVERSE_Z?0.0f:1.0f, 0);
        	VkRect2D renderArea = renderPassBeginInfo.renderArea();
        	renderAreaExtent = renderArea.extent();
        	renderAreaOffset = renderArea.offset();
        	renderAreaOffset.x(0).y(0);
		}
        void destroy() {
        	memFree(pDescriptorSets);
        	memFree(pOffsets);
        	cmdBufInfo.free();
        	clearValues.free();
        	renderPassBeginInfo.free();
        }
	}
    private void createRenderCommandBuffers(VkCommandBuffer commandBuffer, long framebuffer) {
    	if (initCrap == null) {
    		initCrap = new InitCrap();
    	}
    	int width = vkContext.swapChain.width;
    	int height = vkContext.swapChain.height;
    	initCrap.renderPassBeginInfo.renderPass(vkContext.renderPass);
    	initCrap.renderAreaExtent.set(width, height);
    	initCrap.renderPassBeginInfo.framebuffer(framebuffer);
    	initCrap.pDescriptorSets.put(0, descriptorSets[VKContext.currentBuffer]);
    	initCrap.pOffsets.put(0, 0);
        int err = vkBeginCommandBuffer(commandBuffer, initCrap.cmdBufInfo);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to begin render command buffer: " + VulkanErr.toString(err));
        }
        vkCmdBeginRenderPass(commandBuffer, initCrap.renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
		vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, VkPipelines.main.pipelineLayout, 0, initCrap.pDescriptorSets, null);
		vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, VkPipelines.main.getPtr());
		int idx = update ? VKContext.currentBuffer : 0;
		if(cube[idx].idxCount > 0)
		cube[idx].bindAndDraw(commandBuffer, bufferoffset);
		if(plane[idx].idxCount > 0)
		plane[idx].bindAndDraw(commandBuffer, bufferoffset);
        vkCmdEndRenderPass(commandBuffer);
        err = vkEndCommandBuffer(commandBuffer);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to end render command buffer: " + VulkanErr.toString(err));
        }
    }
	@Override
	public void rebuildRenderCommands() {
		needsRebuildPipeLine = true;
		vkContext.resetRenderCommandPool();
    	int nFrameBuffers = vkContext.swapChain.numImages;
		setupDescriptorSets(vkContext.swapChain.numImages);
		for (int i = 0; this.cube != null && i < cube.length; i++) {
			cube[i].destroy();
			plane[i].destroy();
		}
		this.cube = new VkTesselatorState[vkContext.swapChain.numImages];
		this.plane = new VkTesselatorState[vkContext.swapChain.numImages];
		this.recorded = new boolean[vkContext.swapChain.numImages];
		for (int i = 0; i < plane.length; i++) {
			cube[i] = new VkTesselatorState(vkContext).tag("cube_frame_"+i);
			plane[i] = new VkTesselatorState(vkContext).tag("cube_frame_"+i);
		}
        try ( MemoryStack stack = stackPush() ) {
            VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkInitializers.commandBufferAllocInfo(
            		vkContext.renderCommandPool, nFrameBuffers);
        
            PointerBuffer pCommandBuffer = stack.callocPointer(nFrameBuffers);
            int err = vkAllocateCommandBuffers(vkContext.device, cmdBufAllocateInfo, pCommandBuffer);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to allocate render command buffer: " + VulkanErr.toString(err));
            }
            renderCommandBuffers = new VkCommandBuffer[nFrameBuffers];
            for (int i = 0; i < nFrameBuffers; i++) {
                renderCommandBuffers[i] = new VkCommandBuffer(pCommandBuffer.get(i), vkContext.device);
            }
        }

	}

	private void destroyCommandBuffers() {
    	if (renderCommandBuffers != null) {
    		for (int i = 0; i < renderCommandBuffers.length; i++) {
    			VkCommandBuffer cmdBuf = renderCommandBuffers[i];
    			vkFreeCommandBuffers(vkContext.device, vkContext.renderCommandPool, cmdBuf);
    		}
    		renderCommandBuffers = null;
    	}
	}
	public void shutdown() {
    	destroyCommandBuffers();
    	initCrap.destroy();
    	vkDestroyDescriptorSetLayout(vkContext.device, descriptorSetLayout, null);
    	vkDestroyDescriptorPool(vkContext.device, descriptorPool, null);
    	VkPipelines.destroyShutdown(vkContext);
    	vkDestroyImageView(vkContext.device, texture.view, null);
    	vkDestroySampler(vkContext.device, sampler, null);
    	vkDestroyImage(vkContext.device, texture.image, null);
    	vkContext.memoryManager.releaseImageMemory(texture.image);
    	super.shutdown();
    }
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
	}
}
