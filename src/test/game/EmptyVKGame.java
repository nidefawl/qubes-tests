package test.game;


import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.VK10.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.util.Stats;
import nidefawl.qubes.vulkan.*;

public class EmptyVKGame extends GameBase {
	static {
		System.setProperty("renderer.vulkan", "true");
	}
	private VkCommandBuffer[] renderCommandBuffers;
	public EmptyVKGame() {
		TICKS_PER_SEC = 20;
		DEBUG_LAYER = true;
	}
	public static void main(String[] args) {

		new EmptyVKGame().startGame();
	}
	

	@Override
	public void onStatsUpdated() {
		System.out.println(lastFPS+" ("+String.format("%.5fms", Stats.avgFrameTime)+")");
	}

	@Override
	protected void onTextInput(long window, int codepoint) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onKeyPress(long window, int key, int scancode, int action, int mods) {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(float f) {
		int currentBuffer = VKContext.currentBuffer;
		this.vkContext.submitCommandBuffer(renderCommandBuffers[currentBuffer]);

        // Submit to the graphics queue
        int err = vkQueueSubmit(this.vkContext.vkQueue, this.vkContext.submitInfo, VK_NULL_HANDLE);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to submit render queue: " + VulkanErr.toString(err));
        }
	}

	@Override
	public void preRenderUpdate(float f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void postRenderUpdate(float f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setRenderResolution(int displayWidth, int displayHeight) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tick() {
		
	}

	@Override
	public void initGame() {
		// TODO Auto-generated method stub
		setVSync(false);
	}

	@Override
	public void lateInitGame() {
		// TODO Auto-generated method stub


	}


	/* (non-Javadoc)
	 * @see nidefawl.qubes.GameBase#onWheelScroll(long, double, double)
	 */
	@Override
	protected void onWheelScroll(long window, double xoffset, double yoffset) {
		// TODO Auto-generated method stub
		
	}

    private VkCommandBuffer[] createRenderCommandBuffers() {
    	long[] fbSwapchain = vkContext.swapChain.framebuffers;
    	int nFrameBuffers = fbSwapchain.length;
    	int width = vkContext.swapChain.width;
    	int height = vkContext.swapChain.height;
        // Create the render command buffers (one command buffer per framebuffer image)
        VkCommandBufferAllocateInfo cmdBufAllocateInfo = VkCommandBufferAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(vkContext.renderCommandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(nFrameBuffers);
        PointerBuffer pCommandBuffer = memAllocPointer(nFrameBuffers);
        int err = vkAllocateCommandBuffers(vkContext.device, cmdBufAllocateInfo, pCommandBuffer);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to allocate render command buffer: " + VulkanErr.toString(err));
        }
        VkCommandBuffer[] renderCommandBuffers = new VkCommandBuffer[nFrameBuffers];
        for (int i = 0; i < nFrameBuffers; i++) {
            renderCommandBuffers[i] = new VkCommandBuffer(pCommandBuffer.get(i), vkContext.device);
        }
        memFree(pCommandBuffer);
        cmdBufAllocateInfo.free();

        // Create the command buffer begin structure
        VkCommandBufferBeginInfo cmdBufInfo = VkCommandBufferBeginInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .pNext(NULL);

        // Specify clear color (cornflower blue)
        VkClearValue.Buffer clearValues = VkClearValue.calloc(1);
        clearValues.color()
                .float32(0, 100/255.0f)
                .float32(1, 119/255.0f)
                .float32(2, 037/255.0f)
                .float32(3, 1.0f);

        // Specify everything to begin a render pass
        VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .pNext(NULL)
                .renderPass(VkRenderPasses.passSubpassSwapchain.get())
                .pClearValues(clearValues);
        VkRect2D renderArea = renderPassBeginInfo.renderArea();
        renderArea.offset()
                .x(0)
                .y(0);
        renderArea.extent()
                .width(width)
                .height(height);

        for (int i = 0; i < renderCommandBuffers.length; ++i) {
            // Set target frame buffer
            renderPassBeginInfo.framebuffer(fbSwapchain[i]);

            err = vkBeginCommandBuffer(renderCommandBuffers[i], cmdBufInfo);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to begin render command buffer: " + VulkanErr.toString(err));
            }

            vkCmdBeginRenderPass(renderCommandBuffers[i], renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            // Update dynamic viewport state
            VkViewport.Buffer viewport = VkViewport.calloc(1)
                    .height(height)
                    .width(width)
                    .minDepth(0.0f)
                    .maxDepth(1.0f);
            vkCmdSetViewport(renderCommandBuffers[i], 0, viewport);
            viewport.free();

            // Update dynamic scissor state
            VkRect2D.Buffer scissor = VkRect2D.calloc(1);
            scissor.extent()
                    .width(width)
                    .height(height);
            scissor.offset()
                    .x(0)
                    .y(0);
            vkCmdSetScissor(renderCommandBuffers[i], 0, scissor);
            scissor.free();

            vkCmdEndRenderPass(renderCommandBuffers[i]);


            err = vkEndCommandBuffer(renderCommandBuffers[i]);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to begin render command buffer: " + VulkanErr.toString(err));
            }
        }
        renderPassBeginInfo.free();
        clearValues.free();
        cmdBufInfo.free();
        return renderCommandBuffers;
    }
	@Override
	public void rebuildRenderCommands(int width, int height) {
		vkContext.resetRenderCommandPool();
		destroyCommandBuffers();
        renderCommandBuffers = createRenderCommandBuffers();

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
    	super.shutdown();
    }

}
