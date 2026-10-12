/**
 * MoonCake — Luxury 3D Gift Box Unboxing Experience V2
 * Art Direction: Moonlit Gift Presentation
 * High-fidelity Three.js procedural packaging & sculpted traditional mooncakes.
 */

import * as THREE from './three.module.min.js';

(function () {
  'use strict';

  // Section & DOM element references
  const section = document.getElementById('kham-pha-hop-qua');
  const canvasContainer = document.getElementById('gift-box-canvas-container');
  const loader = document.getElementById('box-3d-loader');
  const fallback = document.getElementById('box-3d-fallback');
  const replayBtn = document.getElementById('box-replay-btn');
  const interactionHint = document.getElementById('box-interaction-hint');

  if (!section || !canvasContainer) {
    return;
  }

  // Accessibility check: prefers-reduced-motion
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  // WebGL Support Detection
  function isWebGLAvailable() {
    try {
      const canvas = document.createElement('canvas');
      return !!(window.WebGLRenderingContext && (canvas.getContext('webgl') || canvas.getContext('experimental-webgl')));
    } catch (e) {
      return false;
    }
  }

  if (!isWebGLAvailable()) {
    showFallback();
    return;
  }

  function showFallback() {
    if (loader) loader.style.display = 'none';
    if (fallback) fallback.classList.remove('hidden');
    if (replayBtn) replayBtn.style.display = 'none';
  }

  // State Machine: 'uninitialized' -> 'ready' -> 'opening' -> 'open' -> 'replaying' -> 'fallback'
  let state = 'uninitialized';

  // Three.js Core Objects
  let scene, camera, renderer;
  let boxRootGroup, lidHingeGroup;
  let keyLight, fillLight, rimLight, interiorLight;
  let initialCameraPos = new THREE.Vector3(0.2, 4.3, 8.2);
  let openCameraPos = new THREE.Vector3(0.0, 4.5, 7.3);
  let targetLookAt = new THREE.Vector3(0, 0.55, 0);

  // Animation Timeline Parameters
  const TARGET_LID_ANGLE = Math.PI * 0.44; // ~79.2 degrees
  const ANIMATION_DURATION = 3000; // 3.0 seconds
  let animStartTime = 0;
  let isAnimating = false;
  let animationId = null;

  // Pointer Interaction Variables
  let targetRotationY = 0;
  let targetRotationX = 0;
  let currentRotationY = 0;
  let currentRotationX = 0;
  let isPointerActive = false;
  let pointerRafId = null;
  let hasInteractedWithMouse = false;

  // Texture Cache
  let emblemTexture = null;
  let pastryBumpTexture = null;

  // Generate Organic Baked Pastry Stipple/Bump Canvas Texture
  function createPastryTexture() {
    const size = 512;
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');

    // Base warm golden baked tone
    ctx.fillStyle = '#b36d22';
    ctx.fillRect(0, 0, size, size);

    // Subtle baked brown gradient rings
    const grad = ctx.createRadialGradient(size / 2, size / 2, size * 0.1, size / 2, size / 2, size * 0.5);
    grad.addColorStop(0, 'rgba(230, 160, 60, 0.4)');
    grad.addColorStop(0.7, 'rgba(140, 70, 15, 0.3)');
    grad.addColorStop(1, 'rgba(95, 45, 10, 0.6)');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, size, size);

    // Fine organic stippling (baked crust pores & flour dusting)
    for (let i = 0; i < 4000; i++) {
      const x = Math.random() * size;
      const y = Math.random() * size;
      const r = Math.random() * 1.5 + 0.5;
      const alpha = Math.random() * 0.18 + 0.02;
      ctx.fillStyle = Math.random() > 0.4 ? `rgba(255, 235, 180, ${alpha})` : `rgba(60, 25, 5, ${alpha * 1.4})`;
      ctx.beginPath();
      ctx.arc(x, y, r, 0, Math.PI * 2);
      ctx.fill();
    }

    const texture = new THREE.CanvasTexture(canvas);
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    texture.repeat.set(2, 2);
    return texture;
  }

  // Initialize Scene, Renderer, and Assets
  function init3DScene() {
    const width = canvasContainer.clientWidth || 700;
    const height = canvasContainer.clientHeight || 580;

    // 1. Scene
    scene = new THREE.Scene();
    scene.background = null;

    // 2. Camera
    camera = new THREE.PerspectiveCamera(34, width / height, 0.1, 50);
    camera.position.copy(initialCameraPos);
    camera.lookAt(targetLookAt);

    // 3. Renderer
    renderer = new THREE.WebGLRenderer({
      antialias: true,
      alpha: true,
      powerPreference: 'high-performance'
    });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.18;

    canvasContainer.appendChild(renderer.domElement);

    // Generate procedural bump texture for baked crust
    pastryBumpTexture = createPastryTexture();

    // Load authentic MoonCake V4 Emblem Texture
    const textureLoader = new THREE.TextureLoader();
    textureLoader.load(
      '/images/branding/mooncake_luxury_emblem_v4.png',
      (tex) => {
        emblemTexture = tex;
        emblemTexture.colorSpace = THREE.SRGBColorSpace;
        applyEmblemToLid();
        renderer.render(scene, camera);
      },
      undefined,
      (err) => {
        console.warn('MoonCake emblem texture fallback active:', err);
      }
    );

    // 4. Lighting Rig (High-end Jewelry & Packaging Product Studio)
    setupLighting();

    // 5. Build Luxury Moonlit Product Dais / Pedestal
    buildDisplayStage();

    // 6. Build High-Fidelity 3D Gift Box & Sculpted Mooncakes
    buildLuxuryGiftBox();

    // If reduced motion is requested, show final open state immediately
    if (prefersReducedMotion) {
      lidHingeGroup.rotation.x = -TARGET_LID_ANGLE;
      camera.position.copy(openCameraPos);
      camera.lookAt(targetLookAt);
      state = 'open';
      if (loader) loader.style.opacity = '0';
      setTimeout(() => { if (loader) loader.style.display = 'none'; }, 400);
      renderer.render(scene, camera);
      return;
    }

    state = 'ready';
    renderer.render(scene, camera);

    if (loader) {
      loader.style.opacity = '0';
      setTimeout(() => { loader.style.display = 'none'; }, 400);
    }

    // Attach Event Listeners
    setupPointerInteraction();
    window.addEventListener('resize', onWindowResize, { passive: true });
  }

  // Lighting Setup: Soft Warm Key + Cool Indigo Fill + High-Angle Gold Rim + Interior Soft Spotlight
  function setupLighting() {
    // Ambient Light (subtle midnight navy fill)
    const ambientLight = new THREE.AmbientLight(0x1a2942, 1.15);
    scene.add(ambientLight);

    // Key Light (warm ivory-gold spotlight)
    keyLight = new THREE.DirectionalLight(0xffedd1, 2.5);
    keyLight.position.set(4.8, 6.8, 5.2);
    keyLight.castShadow = true;
    keyLight.shadow.mapSize.width = 2048;
    keyLight.shadow.mapSize.height = 2048;
    keyLight.shadow.camera.near = 0.5;
    keyLight.shadow.camera.far = 20;
    keyLight.shadow.bias = -0.0004;
    const d = 4.2;
    keyLight.shadow.camera.left = -d;
    keyLight.shadow.camera.right = d;
    keyLight.shadow.camera.top = d;
    keyLight.shadow.camera.bottom = -d;
    scene.add(keyLight);

    // Fill Light (cool moonlight fill)
    fillLight = new THREE.DirectionalLight(0x7c9dcc, 0.95);
    fillLight.position.set(-5.2, 3.8, 2.5);
    scene.add(fillLight);

    // Rim Light (warm metallic gold backlight)
    rimLight = new THREE.DirectionalLight(0xd1a85a, 1.8);
    rimLight.position.set(-2.5, 5.5, -4.5);
    scene.add(rimLight);

    // Interior Accent Light (illuminates the revealed pastries and gold tray once opened)
    interiorLight = new THREE.SpotLight(0xffe4b5, 0, 14, Math.PI / 3.2, 0.45, 1.2);
    interiorLight.position.set(0, 5.4, 0.8);
    interiorLight.target.position.set(0, 0.4, 0);
    scene.add(interiorLight.target);
    scene.add(interiorLight);
  }

  // Display Pedestal (Moonlit Product Stage)
  function buildDisplayStage() {
    const stageGroup = new THREE.Group();
    stageGroup.position.set(0, -0.06, 0);

    // Circular Lacquer Pedestal
    const daisGeo = new THREE.CylinderGeometry(4.2, 4.25, 0.09, 64);
    const daisMat = new THREE.MeshStandardMaterial({
      color: 0x081220,
      roughness: 0.35,
      metalness: 0.25
    });
    const daisMesh = new THREE.Mesh(daisGeo, daisMat);
    daisMesh.receiveShadow = true;
    stageGroup.add(daisMesh);

    // Gold Beveled Edge Ring on Pedestal
    const ringGeo = new THREE.TorusGeometry(4.23, 0.022, 12, 64);
    const ringMat = new THREE.MeshStandardMaterial({
      color: 0xd1a85a,
      roughness: 0.22,
      metalness: 0.88
    });
    const ringMesh = new THREE.Mesh(ringGeo, ringMat);
    ringMesh.rotation.x = Math.PI / 2;
    ringMesh.position.y = 0.045;
    stageGroup.add(ringMesh);

    // Ground Shadow Catcher under stage
    const shadowGeo = new THREE.PlaneGeometry(14, 12);
    const shadowMat = new THREE.ShadowMaterial({ opacity: 0.42 });
    const shadowPlane = new THREE.Mesh(shadowGeo, shadowMat);
    shadowPlane.rotation.x = -Math.PI / 2;
    shadowPlane.position.y = -0.05;
    shadowPlane.receiveShadow = true;
    stageGroup.add(shadowPlane);

    scene.add(stageGroup);
  }

  let emblemMeshRef = null;

  // Build Detailed Luxury Gift Box & Sculpted Mooncakes
  function buildLuxuryGiftBox() {
    boxRootGroup = new THREE.Group();
    boxRootGroup.position.set(0, 0, 0);
    boxRootGroup.rotation.y = -0.26; // 3/4 perspective
    scene.add(boxRootGroup);

    // PBR Materials
    const navyLacquerMat = new THREE.MeshStandardMaterial({
      color: 0x0e1b2e,
      roughness: 0.24,
      metalness: 0.25
    });

    const goldMetalMat = new THREE.MeshStandardMaterial({
      color: 0xd1a85a,
      roughness: 0.22,
      metalness: 0.88
    });

    const paleGoldMat = new THREE.MeshStandardMaterial({
      color: 0xfaf4a9,
      roughness: 0.28,
      metalness: 0.55
    });

    const satinGoldTrayMat = new THREE.MeshStandardMaterial({
      color: 0xc59a4c,
      roughness: 0.35,
      metalness: 0.65
    });

    const velvetNavyMat = new THREE.MeshStandardMaterial({
      color: 0x112036,
      roughness: 0.78,
      metalness: 0.05
    });

    // Box Proportions
    const boxW = 4.3;
    const boxH = 0.98;
    const boxD = 3.3;
    const wallThick = 0.14;

    const bodyGroup = new THREE.Group();
    boxRootGroup.add(bodyGroup);

    // 1. Double-Tiered Base Plinth (hallmark of luxury lacquer boxes)
    const lowerPlinthGeo = new THREE.BoxGeometry(boxW + 0.12, 0.06, boxD + 0.12);
    const lowerPlinth = new THREE.Mesh(lowerPlinthGeo, navyLacquerMat);
    lowerPlinth.position.set(0, 0.03, 0);
    lowerPlinth.castShadow = true;
    lowerPlinth.receiveShadow = true;
    bodyGroup.add(lowerPlinth);

    const goldRevealGeo = new THREE.BoxGeometry(boxW + 0.04, 0.02, boxD + 0.04);
    const goldReveal = new THREE.Mesh(goldRevealGeo, goldMetalMat);
    goldReveal.position.set(0, 0.07, 0);
    bodyGroup.add(goldReveal);

    // 2. Base Floor
    const baseGeo = new THREE.BoxGeometry(boxW, wallThick, boxD);
    const baseMesh = new THREE.Mesh(baseGeo, navyLacquerMat);
    baseMesh.position.set(0, 0.08 + wallThick / 2, 0);
    baseMesh.castShadow = true;
    baseMesh.receiveShadow = true;
    bodyGroup.add(baseMesh);

    // 3. Walls (Front, Back, Left, Right)
    const frontWallGeo = new THREE.BoxGeometry(boxW, boxH, wallThick);
    const frontWall = new THREE.Mesh(frontWallGeo, navyLacquerMat);
    frontWall.position.set(0, 0.08 + boxH / 2, boxD / 2 - wallThick / 2);
    frontWall.castShadow = true;
    frontWall.receiveShadow = true;
    bodyGroup.add(frontWall);

    const backWall = new THREE.Mesh(frontWallGeo, navyLacquerMat);
    backWall.position.set(0, 0.08 + boxH / 2, -boxD / 2 + wallThick / 2);
    backWall.castShadow = true;
    backWall.receiveShadow = true;
    bodyGroup.add(backWall);

    const sideWallGeo = new THREE.BoxGeometry(wallThick, boxH, boxD - wallThick * 2);
    const leftWall = new THREE.Mesh(sideWallGeo, navyLacquerMat);
    leftWall.position.set(-boxW / 2 + wallThick / 2, 0.08 + boxH / 2, 0);
    leftWall.castShadow = true;
    leftWall.receiveShadow = true;
    bodyGroup.add(leftWall);

    const rightWall = new THREE.Mesh(sideWallGeo, navyLacquerMat);
    rightWall.position.set(boxW / 2 - wallThick / 2, 0.08 + boxH / 2, 0);
    rightWall.castShadow = true;
    rightWall.receiveShadow = true;
    bodyGroup.add(rightWall);

    // 4. Beveled Gold Trim along top rim of box body
    const rimFrontGeo = new THREE.BoxGeometry(boxW + 0.02, 0.03, wallThick + 0.02);
    const rimFront = new THREE.Mesh(rimFrontGeo, goldMetalMat);
    rimFront.position.set(0, 0.08 + boxH + 0.015, boxD / 2 - wallThick / 2);
    bodyGroup.add(rimFront);

    const rimBack = new THREE.Mesh(rimFrontGeo, goldMetalMat);
    rimBack.position.set(0, 0.08 + boxH + 0.015, -boxD / 2 + wallThick / 2);
    bodyGroup.add(rimBack);

    const rimSideGeo = new THREE.BoxGeometry(wallThick + 0.02, 0.03, boxD - wallThick * 2);
    const rimLeft = new THREE.Mesh(rimSideGeo, goldMetalMat);
    rimLeft.position.set(-boxW / 2 + wallThick / 2, 0.08 + boxH + 0.015, 0);
    bodyGroup.add(rimLeft);

    const rimRight = new THREE.Mesh(rimSideGeo, goldMetalMat);
    rimRight.position.set(boxW / 2 - wallThick / 2, 0.08 + boxH + 0.015, 0);
    bodyGroup.add(rimRight);

    // 5. Interior Satin Gold Tray & Compartments
    const trayW = boxW - wallThick * 2 - 0.04;
    const trayD = boxD - wallThick * 2 - 0.04;
    const trayH = 0.28;

    const trayBaseGeo = new THREE.BoxGeometry(trayW, trayH, trayD);
    const trayBase = new THREE.Mesh(trayBaseGeo, satinGoldTrayMat);
    trayBase.position.set(0, 0.08 + wallThick + trayH / 2, 0);
    trayBase.receiveShadow = true;
    bodyGroup.add(trayBase);

    // Gold Dividers with Chamfered Fillets
    const divXGeo = new THREE.BoxGeometry(0.07, 0.42, trayD);
    const divX = new THREE.Mesh(divXGeo, goldMetalMat);
    divX.position.set(0, 0.08 + wallThick + 0.21, 0);
    divX.castShadow = true;
    bodyGroup.add(divX);

    const divZGeo = new THREE.BoxGeometry(trayW, 0.42, 0.07);
    const divZ = new THREE.Mesh(divZGeo, goldMetalMat);
    divZ.position.set(0, 0.08 + wallThick + 0.21, 0);
    divZ.castShadow = true;
    bodyGroup.add(divZ);

    // 6. BUILD FOUR SCULPTED AUTHENTIC MOONCAKES
    const compartmentX = trayW / 4;
    const compartmentZ = trayD / 4;
    const mooncakeBaseY = 0.08 + wallThick + trayH + 0.01;

    const mooncakeConfigs = [
      { x: -compartmentX, z: -compartmentZ, lobes: 16, petals: 8, crustColor: 0xa4611e, sealColor: 0xd9953d, name: 'Thập Cẩm XO' },
      { x: compartmentX, z: -compartmentZ, lobes: 12, petals: 12, crustColor: 0xb87326, sealColor: 0xefa744, name: 'Lava Hoàng Kim' },
      { x: -compartmentX, z: compartmentZ, lobes: 16, petals: 10, crustColor: 0x985517, sealColor: 0xd08a34, name: 'Hạt Sen Trà Xanh' },
      { x: compartmentX, z: compartmentZ, lobes: 12, petals: 8, crustColor: 0x8a4a12, sealColor: 0xc47e2b, name: 'Thập Cẩm Bát Bửu' }
    ];

    mooncakeConfigs.forEach((cfg) => {
      const mcGroup = createSculptedMooncake(cfg, goldMetalMat);
      mcGroup.position.set(cfg.x, mooncakeBaseY, cfg.z);
      bodyGroup.add(mcGroup);
    });

    // 7. SEPARATE HINGED LID (Geometrically exact back-edge pivot)
    // Pivot at rear top edge: (0, 0.08 + boxH + 0.02, -boxD / 2)
    const hingeY = 0.08 + boxH + 0.025;
    const hingeZ = -boxD / 2;

    lidHingeGroup = new THREE.Group();
    lidHingeGroup.position.set(0, hingeY, hingeZ);
    boxRootGroup.add(lidHingeGroup);

    // Lid geometry
    const lidW = boxW + 0.08;
    const lidD = boxD + 0.08;
    const lidThick = 0.16;

    const lidMeshGroup = new THREE.Group();
    // Offset lid so rear bottom edge is at hinge origin (0, 0, 0)
    lidMeshGroup.position.set(0, lidThick / 2, lidD / 2);
    lidHingeGroup.add(lidMeshGroup);

    // Lid Main Plate (Beveled lacquer rigid board)
    const lidPlateGeo = new THREE.BoxGeometry(lidW, lidThick, lidD);
    const lidPlate = new THREE.Mesh(lidPlateGeo, navyLacquerMat);
    lidPlate.castShadow = true;
    lidPlate.receiveShadow = true;
    lidMeshGroup.add(lidPlate);

    // Physical Brass Butt Hinges on Rear
    const hingeLeafGeo = new THREE.BoxGeometry(0.35, 0.06, 0.08);
    const hingeLeft = new THREE.Mesh(hingeLeafGeo, goldMetalMat);
    hingeLeft.position.set(-1.2, 0, -lidD / 2 + 0.04);
    lidMeshGroup.add(hingeLeft);

    const hingeRight = new THREE.Mesh(hingeLeafGeo, goldMetalMat);
    hingeRight.position.set(1.2, 0, -lidD / 2 + 0.04);
    lidMeshGroup.add(hingeRight);

    // Gold Outer Perimeter Pinstripe Inlay
    const outerFrameGeo = new THREE.BoxGeometry(lidW - 0.32, 0.015, lidD - 0.32);
    const outerFrame = new THREE.Mesh(outerFrameGeo, goldMetalMat);
    outerFrame.position.set(0, lidThick / 2 + 0.008, 0);
    lidMeshGroup.add(outerFrame);

    const outerFrameCutGeo = new THREE.BoxGeometry(lidW - 0.38, 0.02, lidD - 0.38);
    const outerFrameCut = new THREE.Mesh(outerFrameCutGeo, navyLacquerMat);
    outerFrameCut.position.set(0, lidThick / 2 + 0.009, 0);
    lidMeshGroup.add(outerFrameCut);

    // Gold Inner Secondary Pinstripe Inlay
    const innerFrameGeo = new THREE.BoxGeometry(lidW - 0.65, 0.015, lidD - 0.65);
    const innerFrame = new THREE.Mesh(innerFrameGeo, goldMetalMat);
    innerFrame.position.set(0, lidThick / 2 + 0.008, 0);
    lidMeshGroup.add(innerFrame);

    const innerFrameCutGeo = new THREE.BoxGeometry(lidW - 0.69, 0.02, lidD - 0.69);
    const innerFrameCut = new THREE.Mesh(innerFrameCutGeo, navyLacquerMat);
    innerFrameCut.position.set(0, lidThick / 2 + 0.009, 0);
    lidMeshGroup.add(innerFrameCut);

    // Four Symmetrical Gold Corner Filigree Ornaments
    const cornerOffsets = [
      { x: (lidW - 0.65) / 2, z: (lidD - 0.65) / 2, rot: 0 },
      { x: -(lidW - 0.65) / 2, z: (lidD - 0.65) / 2, rot: Math.PI / 2 },
      { x: -(lidW - 0.65) / 2, z: -(lidD - 0.65) / 2, rot: Math.PI },
      { x: (lidW - 0.65) / 2, z: -(lidD - 0.65) / 2, rot: -Math.PI / 2 }
    ];

    cornerOffsets.forEach((co) => {
      const cornerBracket = new THREE.Group();
      cornerBracket.position.set(co.x, lidThick / 2 + 0.01, co.z);
      cornerBracket.rotation.y = co.rot;

      const arm1 = new THREE.Mesh(new THREE.BoxGeometry(0.24, 0.012, 0.035), goldMetalMat);
      arm1.position.set(-0.12, 0, 0);
      cornerBracket.add(arm1);

      const arm2 = new THREE.Mesh(new THREE.BoxGeometry(0.035, 0.012, 0.24), goldMetalMat);
      arm2.position.set(0, 0, -0.12);
      cornerBracket.add(arm2);

      const dot = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 0.016, 12), goldMetalMat);
      dot.position.set(-0.08, 0, -0.08);
      cornerBracket.add(dot);

      lidMeshGroup.add(cornerBracket);
    });

    // 8. CENTER MOONCAKE V4 EMBLEM MEDALLION ON LID
    // Scalloped Gold Medallion Base
    const medallionGeo = new THREE.CylinderGeometry(0.78, 0.78, 0.035, 36);
    const medallionMesh = new THREE.Mesh(medallionGeo, paleGoldMat);
    medallionMesh.position.set(0, lidThick / 2 + 0.018, 0);
    medallionMesh.castShadow = true;
    lidMeshGroup.add(medallionMesh);

    // Raised Outer Gold Bezel
    const bezelGeo = new THREE.TorusGeometry(0.78, 0.028, 12, 40);
    const bezelMesh = new THREE.Mesh(bezelGeo, goldMetalMat);
    bezelMesh.rotation.x = Math.PI / 2;
    bezelMesh.position.set(0, lidThick / 2 + 0.036, 0);
    lidMeshGroup.add(bezelMesh);

    // Authentic V4 Logo Texture Plane
    const emblemPlaneGeo = new THREE.PlaneGeometry(1.42, 1.42);
    const emblemPlaneMat = new THREE.MeshStandardMaterial({
      color: 0xffffff,
      transparent: true,
      alphaTest: 0.02,
      roughness: 0.35,
      metalness: 0.3,
      emissive: 0xd1a85a,
      emissiveIntensity: 0.15, // Subtle warm gold luminance to keep the emblem clear and readable
      polygonOffset: true,
      polygonOffsetFactor: -1,
      polygonOffsetUnits: -1
    });

    emblemMeshRef = new THREE.Mesh(emblemPlaneGeo, emblemPlaneMat);
    emblemMeshRef.rotation.x = -Math.PI / 2;
    emblemMeshRef.position.set(0, lidThick / 2 + 0.038, 0);
    lidMeshGroup.add(emblemMeshRef);

    // 9. UNDERSIDE OF LID (Revealed when opened)
    // Velvet Lining Panel
    const undersideVelvetGeo = new THREE.BoxGeometry(lidW - 0.16, 0.012, lidD - 0.16);
    const undersideVelvet = new THREE.Mesh(undersideVelvetGeo, velvetNavyMat);
    undersideVelvet.position.set(0, -lidThick / 2 - 0.007, 0);
    lidMeshGroup.add(undersideVelvet);

    // Underside Gold Foil Perimeter
    const innerLidGoldRimGeo = new THREE.BoxGeometry(lidW - 0.32, 0.015, lidD - 0.32);
    const innerLidGoldRim = new THREE.Mesh(innerLidGoldRimGeo, goldMetalMat);
    innerLidGoldRim.position.set(0, -lidThick / 2 - 0.01, 0);
    lidMeshGroup.add(innerLidGoldRim);

    const innerLidCutGeo = new THREE.BoxGeometry(lidW - 0.40, 0.018, lidD - 0.40);
    const innerLidCut = new THREE.Mesh(innerLidCutGeo, velvetNavyMat);
    innerLidCut.position.set(0, -lidThick / 2 - 0.011, 0);
    lidMeshGroup.add(innerLidCut);

    // Underside Reflective Gold Foil Medallion
    const innerSealGeo = new THREE.CylinderGeometry(0.62, 0.62, 0.018, 36);
    const innerSeal = new THREE.Mesh(innerSealGeo, goldMetalMat);
    innerSeal.rotation.x = Math.PI; // Face downward when closed, upward when open
    innerSeal.position.set(0, -lidThick / 2 - 0.015, 0);
    lidMeshGroup.add(innerSeal);

    // Initial state: Lid completely closed
    lidHingeGroup.rotation.x = 0;
  }

  // Apply V4 Emblem texture once loaded
  function applyEmblemToLid() {
    if (emblemMeshRef && emblemTexture) {
      emblemMeshRef.material.map = emblemTexture;
      emblemMeshRef.material.needsUpdate = true;
    }
  }

  /**
   * Creates an intricate traditional mooncake with:
   * 1. Multi-lobed scalloped extruded body (vertical sidewall flutes).
   * 2. Molded top relief with concentric floral petals.
   * 3. Authentic baked crust PBR material with bump stippling.
   * 4. Pleated metallic gold foil cup liner.
   */
  function createSculptedMooncake(cfg, goldMetalMat) {
    const mcGroup = new THREE.Group();

    // 1. Pleated Metallic Gold Cup Liner
    const cupGeo = new THREE.CylinderGeometry(0.66, 0.58, 0.14, cfg.lobes);
    const cupMesh = new THREE.Mesh(cupGeo, goldMetalMat);
    cupMesh.position.y = 0.07;
    cupMesh.receiveShadow = true;
    mcGroup.add(cupMesh);

    // 2. Multi-lobed Scalloped Pastry Contour Shape
    const shape = new THREE.Shape();
    const lobes = cfg.lobes;
    const baseRadius = 0.58;
    const scallopDepth = 0.048;
    const segments = lobes * 8;

    for (let i = 0; i <= segments; i++) {
      const theta = (i / segments) * Math.PI * 2;
      const r = baseRadius + Math.cos(lobes * theta) * scallopDepth;
      const x = Math.cos(theta) * r;
      const y = Math.sin(theta) * r;
      if (i === 0) shape.moveTo(x, y);
      else shape.lineTo(x, y);
    }
    shape.closePath();

    // Extrude with smooth rounded bevels
    const pastryDepth = 0.34;
    const bevelThick = 0.035;
    const pastryGeo = new THREE.ExtrudeGeometry(shape, {
      depth: pastryDepth,
      bevelEnabled: true,
      bevelSegments: 4,
      steps: 1,
      bevelSize: 0.035,
      bevelThickness: bevelThick
    });
    // Lay pastry horizontal: rotateX(PI/2) puts the front face (z = -bevelThick) at y = bevelThick
    pastryGeo.rotateX(Math.PI / 2);

    // Baked Crust PBR Material
    const pastryMat = new THREE.MeshStandardMaterial({
      color: cfg.crustColor,
      bumpMap: pastryBumpTexture,
      bumpScale: 0.014,
      roughness: 0.58,
      metalness: 0.04
    });

    const pastryMesh = new THREE.Mesh(pastryGeo, pastryMat);
    pastryMesh.position.y = pastryDepth + bevelThick;
    pastryMesh.castShadow = true;
    pastryMesh.receiveShadow = true;
    mcGroup.add(pastryMesh);

    // Top surface of pastry is at y = pastryDepth + 2 * bevelThick = 0.41
    const topY = pastryDepth + bevelThick * 2;

    // 3. Molded Top Floral Relief Decoration (Rests right on top surface!)
    const decorGroup = new THREE.Group();
    decorGroup.position.y = topY;
    mcGroup.add(decorGroup);

    // Glazed Crust Material for molded relief (caramelized egg-wash sheen)
    const glazedMat = new THREE.MeshStandardMaterial({
      color: cfg.sealColor,
      bumpMap: pastryBumpTexture,
      bumpScale: 0.010,
      roughness: 0.38,
      metalness: 0.15
    });

    // Raised Outer Scalloped Framing Rim
    const outerRingGeo = new THREE.TorusGeometry(0.46, 0.022, 12, 36);
    const outerRing = new THREE.Mesh(outerRingGeo, glazedMat);
    outerRing.rotation.x = Math.PI / 2;
    outerRing.position.y = 0.012;
    outerRing.castShadow = true;
    decorGroup.add(outerRing);

    // Inner Concentric Beaded Ring
    const innerRingGeo = new THREE.TorusGeometry(0.26, 0.018, 10, 30);
    const innerRing = new THREE.Mesh(innerRingGeo, glazedMat);
    innerRing.rotation.x = Math.PI / 2;
    innerRing.position.y = 0.015;
    innerRing.castShadow = true;
    decorGroup.add(innerRing);

    // Molded Petal Relief (Traditional Lotus Pastry Motif)
    const petalCount = cfg.petals;
    const petalShape = new THREE.Shape();
    petalShape.moveTo(0, 0.12);
    petalShape.bezierCurveTo(0.065, 0.22, 0.075, 0.33, 0, 0.44);
    petalShape.bezierCurveTo(-0.075, 0.33, -0.065, 0.22, 0, 0.12);

    const petalGeo = new THREE.ExtrudeGeometry(petalShape, {
      depth: 0.022,
      bevelEnabled: true,
      bevelSegments: 2,
      steps: 1,
      bevelSize: 0.008,
      bevelThickness: 0.008
    });
    // Rotate so extrusion goes UPWARDS (+Y)
    petalGeo.rotateX(-Math.PI / 2);

    for (let p = 0; p < petalCount; p++) {
      const angle = (p / petalCount) * Math.PI * 2;
      const petalMesh = new THREE.Mesh(petalGeo, glazedMat);
      petalMesh.rotation.y = angle;
      petalMesh.position.y = 0.006;
      petalMesh.castShadow = true;
      decorGroup.add(petalMesh);
    }

    // Center Raised Core Medallion
    const centerCoreGeo = new THREE.CylinderGeometry(0.13, 0.13, 0.03, 24);
    const centerCore = new THREE.Mesh(centerCoreGeo, glazedMat);
    centerCore.position.y = 0.02;
    centerCore.castShadow = true;
    decorGroup.add(centerCore);

    const centerBeadGeo = new THREE.SphereGeometry(0.045, 14, 14);
    const centerBead = new THREE.Mesh(centerBeadGeo, goldMetalMat);
    centerBead.position.y = 0.045;
    decorGroup.add(centerBead);

    return mcGroup;
  }

  // Smooth Easing Function (easeInOutCubic)
  function easeInOutCubic(x) {
    return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
  }

  // Start Opening Animation (Deterministic 3-second sequence)
  function startOpeningAnimation() {
    if (state === 'opening' || isAnimating) return;

    state = 'opening';
    isAnimating = true;
    animStartTime = performance.now();

    if (replayBtn) {
      replayBtn.disabled = true;
    }

    if (interactionHint) {
      interactionHint.classList.add('opacity-0');
    }

    animateFrame();
  }

  // Main Deterministic Animation Loop
  function animateFrame() {
    if (!isAnimating) return;

    const now = performance.now();
    const elapsed = now - animStartTime;
    const progress = Math.min(elapsed / ANIMATION_DURATION, 1.0);

    // PHASE 1: 0.0 - 0.5s (progress 0.0 to 0.167) — Gentle Grounding Entrance
    if (progress < 0.167) {
      const p1 = progress / 0.167;
      const ease1 = easeInOutCubic(p1);
      boxRootGroup.position.y = (1 - ease1) * -0.15;
      boxRootGroup.rotation.y = -0.30 + ease1 * 0.04;
      lidHingeGroup.rotation.x = 0;
      camera.position.copy(initialCameraPos);
      keyLight.intensity = 2.5;
      if (interiorLight) interiorLight.intensity = 0;
    }
    // PHASE 2: 0.5 - 2.0s (progress 0.167 to 0.667) — Smooth Hinged Lid Opening
    else if (progress < 0.667) {
      boxRootGroup.position.y = 0;
      boxRootGroup.rotation.y = -0.26;
      const p2 = (progress - 0.167) / 0.5;
      const ease2 = easeInOutCubic(p2);
      lidHingeGroup.rotation.x = -ease2 * TARGET_LID_ANGLE;
      // Camera smoothly tracks closer to reveal interior
      camera.position.lerpVectors(initialCameraPos, openCameraPos, ease2 * 0.5);
      camera.lookAt(targetLookAt);
      if (interiorLight) interiorLight.intensity = ease2 * 1.5;
    }
    // PHASE 3: 2.0 - 2.7s (progress 0.667 to 0.9) — Interior Reveal & Lighting Accent
    else if (progress < 0.9) {
      lidHingeGroup.rotation.x = -TARGET_LID_ANGLE;
      const p3 = (progress - 0.667) / 0.233;
      const ease3 = easeInOutCubic(p3);
      camera.position.lerpVectors(
        initialCameraPos.clone().lerp(openCameraPos, 0.5),
        openCameraPos,
        ease3
      );
      camera.lookAt(targetLookAt);
      keyLight.intensity = 2.5 + ease3 * 0.35; // Spotlight warm golden glow on pastries
      if (interiorLight) interiorLight.intensity = 1.5 + ease3 * 0.7; // Reaches 2.2
    }
    // PHASE 4: 2.7 - 3.0s (progress 0.9 to 1.0) — Settle into Final Resting View
    else {
      lidHingeGroup.rotation.x = -TARGET_LID_ANGLE;
      camera.position.copy(openCameraPos);
      camera.lookAt(targetLookAt);
      keyLight.intensity = 2.85;
      if (interiorLight) interiorLight.intensity = 2.2;
    }

    renderer.render(scene, camera);

    if (progress < 1.0) {
      animationId = requestAnimationFrame(animateFrame);
    } else {
      isAnimating = false;
      state = 'open';

      if (replayBtn) {
        replayBtn.disabled = false;
      }

      if (interactionHint && !hasInteractedWithMouse) {
        interactionHint.classList.remove('opacity-0');
      }

      // Stop continuous rendering; switch to on-demand render loop
    }
  }

  // Pointer Interaction (Subtle 3-5 degree responsive parallax)
  function setupPointerInteraction() {
    canvasContainer.addEventListener('pointermove', onPointerMove, { passive: true });
    canvasContainer.addEventListener('pointerleave', onPointerLeave, { passive: true });

    if (replayBtn) {
      replayBtn.addEventListener('click', onReplayClick);
    }
  }

  function onPointerMove(e) {
    if (state !== 'open' || isAnimating) return;

    const rect = canvasContainer.getBoundingClientRect();
    const x = (e.clientX - rect.left) / rect.width - 0.5;
    const y = (e.clientY - rect.top) / rect.height - 0.5;

    // Constrained rotation delta: max ~4 degrees
    targetRotationY = x * 0.08;
    targetRotationX = y * 0.05;

    if (!hasInteractedWithMouse) {
      hasInteractedWithMouse = true;
      if (interactionHint) {
        interactionHint.classList.add('opacity-0');
      }
    }

    if (!isPointerActive) {
      isPointerActive = true;
      renderPointerLoop();
    }
  }

  function onPointerLeave() {
    targetRotationY = 0;
    targetRotationX = 0;
  }

  // Micro-render loop for smooth pointer damping (stops completely when idle!)
  function renderPointerLoop() {
    if (!isPointerActive || isAnimating) return;

    currentRotationY += (targetRotationY - currentRotationY) * 0.07;
    currentRotationX += (targetRotationX - currentRotationX) * 0.07;

    boxRootGroup.rotation.y = -0.26 + currentRotationY;
    boxRootGroup.rotation.x = currentRotationX;

    renderer.render(scene, camera);

    const deltaY = Math.abs(targetRotationY - currentRotationY);
    const deltaX = Math.abs(targetRotationX - currentRotationX);

    // Halt render loop when resting to conserve CPU/GPU
    if (deltaY < 0.0002 && deltaX < 0.0002 && targetRotationY === 0 && targetRotationX === 0) {
      isPointerActive = false;
      boxRootGroup.rotation.y = -0.26;
      boxRootGroup.rotation.x = 0;
      renderer.render(scene, camera);
      return;
    }

    pointerRafId = requestAnimationFrame(renderPointerLoop);
  }

  // Replay Button Action ("Mở lại")
  function onReplayClick() {
    if (isAnimating || state === 'opening') return;

    state = 'replaying';
    isPointerActive = false;
    if (pointerRafId) cancelAnimationFrame(pointerRafId);

    // Reset rotations and camera smoothly
    currentRotationY = 0;
    currentRotationX = 0;
    targetRotationY = 0;
    targetRotationX = 0;
    boxRootGroup.rotation.set(0, -0.26, 0);
    boxRootGroup.position.set(0, 0, 0);
    lidHingeGroup.rotation.x = 0;
    camera.position.copy(initialCameraPos);
    camera.lookAt(targetLookAt);
    keyLight.intensity = 2.5;
    if (interiorLight) interiorLight.intensity = 0;

    renderer.render(scene, camera);

    // Trigger opening sequence after brief reset
    setTimeout(() => {
      startOpeningAnimation();
    }, 120);
  }

  // Window Resize Handling
  function onWindowResize() {
    if (!renderer || !camera || !canvasContainer) return;
    const width = canvasContainer.clientWidth;
    const height = canvasContainer.clientHeight;
    if (width === 0 || height === 0) return;

    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
    renderer.render(scene, camera);
  }

  // IntersectionObserver for Lazy-Loading and Scroll-Trigger
  function setupScrollObserver() {
    // 1. Preload Observer: Initialize WebGL when section approaches (250px margin)
    const preloadObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && state === 'uninitialized') {
            init3DScene();
            preloadObserver.disconnect();
          }
        });
      },
      { rootMargin: '250px 0px 250px 0px', threshold: 0.01 }
    );

    preloadObserver.observe(section);

    // 2. Trigger Animation Observer: Plays sequence when visitor scrolls into view (~0.45 threshold)
    const triggerObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            if (state === 'ready') {
              startOpeningAnimation();
              triggerObserver.disconnect();
            }
          }
        });
      },
      { threshold: 0.45 }
    );

    triggerObserver.observe(section);
  }

  // Bootstrap when DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupScrollObserver);
  } else {
    setupScrollObserver();
  }

})();
