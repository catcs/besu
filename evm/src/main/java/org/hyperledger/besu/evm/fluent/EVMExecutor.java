/*
 * Copyright contributors to Hyperledger Besu
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.evm.fluent;

import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.Code;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.Gas;
import org.hyperledger.besu.evm.MainnetEVMs;
import org.hyperledger.besu.evm.contractvalidation.ContractValidationRule;
import org.hyperledger.besu.evm.contractvalidation.MaxCodeSizeRule;
import org.hyperledger.besu.evm.contractvalidation.PrefixCodeRule;
import org.hyperledger.besu.evm.frame.BlockValues;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.precompile.MainnetPrecompiledContracts;
import org.hyperledger.besu.evm.precompile.PrecompileContractRegistry;
import org.hyperledger.besu.evm.processor.ContractCreationProcessor;
import org.hyperledger.besu.evm.processor.MessageCallProcessor;
import org.hyperledger.besu.evm.tracing.OperationTracer;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

public class EVMExecutor {

  private PrecompileContractRegistry precompileContractRegistry;
  private EVM evm;
  private boolean commitWorldState = false;
  private WorldUpdater worldUpdater = new SimpleWorld();
  private Gas gas = Gas.MAX_VALUE;
  private Address receiver = Address.ZERO;
  private Address sender = Address.ZERO;
  private Wei gasPriceGWei = Wei.ZERO;
  private Bytes callData = Bytes.EMPTY;
  private Wei ethValue = Wei.ZERO;
  private Code code = new Code(Bytes.EMPTY);
  private BlockValues blockValues = new SimpleBlockValues();
  private OperationTracer tracer = OperationTracer.NO_TRACING;
  private boolean requireDeposit = true;
  private List<ContractValidationRule> contractValidationRules =
      List.of(MaxCodeSizeRule.of(0x6000), PrefixCodeRule.of());
  private long initialNonce = 0;
  private Collection<Address> forceCommitAddresses = List.of(Address.fromHexString("0x03"));
  private Set<Address> accessListWarmAddresses = Set.of();
  private Multimap<Address, Bytes32> accessListWarmStorage = HashMultimap.create();
  private MessageCallProcessor messageCallProcessor = null;
  private ContractCreationProcessor contractCreationProcessor = null;

  public static EVMExecutor evm(final EVM evm) {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = evm;
    return executor;
  }

  public static EVMExecutor frontier() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.frontier();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.frontier(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of();
    executor.requireDeposit = false;
    executor.forceCommitAddresses = List.of();
    return executor;
  }

  public static EVMExecutor homestead() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.homestead();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.frontier(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of();
    executor.forceCommitAddresses = List.of();
    return executor;
  }

  public static EVMExecutor spuriousDragon() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.spuriousDragon();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.frontier(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of(MaxCodeSizeRule.of(0x6000));
    return executor;
  }

  public static EVMExecutor tangerineWhistle() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.tangerineWhistle();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.frontier(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of(MaxCodeSizeRule.of(0x6000));
    return executor;
  }

  public static EVMExecutor byzantium() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.byzantium();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.byzantium(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of(MaxCodeSizeRule.of(0x6000));
    return executor;
  }

  public static EVMExecutor constantinople() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.constantinople();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.byzantium(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of(MaxCodeSizeRule.of(0x6000));
    return executor;
  }

  public static EVMExecutor petersburg() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.petersburg();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.byzantium(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of(MaxCodeSizeRule.of(0x6000));
    return executor;
  }

  public static EVMExecutor istanbul() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.istanbul();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.istanbul(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of(MaxCodeSizeRule.of(0x6000));
    return executor;
  }

  public static EVMExecutor berlin() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.berlin();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.istanbul(executor.evm.getGasCalculator());
    executor.contractValidationRules = List.of(MaxCodeSizeRule.of(0x6000));
    return executor;
  }

  public static EVMExecutor london() {
    EVMExecutor executor = new EVMExecutor();
    executor.evm = MainnetEVMs.istanbul();
    executor.precompileContractRegistry =
        MainnetPrecompiledContracts.istanbul(executor.evm.getGasCalculator());
    return executor;
  }

  private MessageCallProcessor thisMessageCallProcessor() {
    return Objects.requireNonNullElseGet(
        messageCallProcessor, () -> new MessageCallProcessor(evm, precompileContractRegistry));
  }

  private ContractCreationProcessor thisContractCreationProcessor() {
    return Objects.requireNonNullElseGet(
        contractCreationProcessor,
        () ->
            new ContractCreationProcessor(
                evm.getGasCalculator(),
                evm,
                requireDeposit,
                contractValidationRules,
                initialNonce,
                forceCommitAddresses));
  }

  public Bytes execute(
      final Code code, final Bytes inputData, final Wei value, final Address receiver) {
    this.code = code;
    this.callData = inputData;
    this.ethValue = value;
    this.receiver = receiver;
    return execute();
  }

  public Bytes execute(
      final Bytes codeBytes, final Bytes inputData, final Wei value, final Address receiver) {
    this.code = new Code(codeBytes);
    this.callData = inputData;
    this.ethValue = value;
    this.receiver = receiver;
    return execute();
  }

  public Bytes execute() {
    MessageCallProcessor mcp = thisMessageCallProcessor();
    ContractCreationProcessor ccp = thisContractCreationProcessor();
    final Deque<MessageFrame> messageFrameStack = new ArrayDeque<>();
    MessageFrame initialMessageFrame =
        MessageFrame.builder()
            .type(MessageFrame.Type.MESSAGE_CALL)
            .messageFrameStack(messageFrameStack)
            .worldUpdater(worldUpdater.updater())
            .initialGas(gas)
            .contract(Address.ZERO)
            .address(receiver)
            .originator(sender)
            .sender(sender)
            .gasPrice(gasPriceGWei)
            .inputData(callData)
            .value(ethValue)
            .apparentValue(ethValue)
            .code(code)
            .blockValues(blockValues)
            .depth(0)
            .completer(c -> {})
            .miningBeneficiary(Address.ZERO)
            .blockHashLookup(h -> null)
            .accessListWarmAddresses(accessListWarmAddresses)
            .accessListWarmStorage(accessListWarmStorage)
            .build();
    messageFrameStack.add(initialMessageFrame);

    while (!messageFrameStack.isEmpty()) {
      final MessageFrame messageFrame = messageFrameStack.peek();
      switch (messageFrame.getType()) {
        case CONTRACT_CREATION:
          mcp.process(messageFrame, tracer);
          break;
        case MESSAGE_CALL:
          ccp.process(messageFrame, tracer);
          break;
      }
    }
    if (commitWorldState) {
      worldUpdater.commit();
    }
    return initialMessageFrame.getReturnData();
  }

  public EVMExecutor commitWorldState() {
    this.commitWorldState = true;
    return this;
  }

  public EVMExecutor commitWorldState(final boolean commitWorldState) {
    this.commitWorldState = commitWorldState;
    return this;
  }

  public EVMExecutor worldUpdater(final WorldUpdater worldUpdater) {
    this.worldUpdater = worldUpdater;
    return this;
  }

  public EVMExecutor gas(final Gas gas) {
    this.gas = gas;
    return this;
  }

  public EVMExecutor receiver(final Address receiver) {
    this.receiver = receiver;
    return this;
  }

  public EVMExecutor sender(final Address sender) {
    this.sender = sender;
    return this;
  }

  public EVMExecutor gasPriceGWei(final Wei gasPriceGWei) {
    this.gasPriceGWei = gasPriceGWei;
    return this;
  }

  public EVMExecutor callData(final Bytes callData) {
    this.callData = callData;
    return this;
  }

  public EVMExecutor ethValue(final Wei ethValue) {
    this.ethValue = ethValue;
    return this;
  }

  public EVMExecutor code(final Code code) {
    this.code = code;
    return this;
  }

  public EVMExecutor code(final Bytes codeBytes) {
    this.code = new Code(codeBytes);
    return this;
  }

  public EVMExecutor blockValues(final BlockValues blockValues) {
    this.blockValues = blockValues;
    return this;
  }

  public EVMExecutor tracer(final OperationTracer tracer) {
    this.tracer = tracer;
    return this;
  }

  public EVMExecutor precompileContractRegistry(
      final PrecompileContractRegistry precompileContractRegistry) {
    this.precompileContractRegistry = precompileContractRegistry;
    return this;
  }

  public EVMExecutor requireDeposit(final boolean requireDeposit) {
    this.requireDeposit = requireDeposit;
    return this;
  }

  public EVMExecutor initialNonce(final long initialNonce) {
    this.initialNonce = initialNonce;
    return this;
  }

  public EVMExecutor contractValidationRules(
      final List<ContractValidationRule> contractValidationRules) {
    this.contractValidationRules = contractValidationRules;
    return this;
  }

  /**
   * List of EIP-718 contracts that require special delete handling. By default, this is only the
   * RIPEMD precompile contract.
   *
   * @see <a
   *     href="https://github.com/ethereum/EIPs/issues/716">https://github.com/ethereum/EIPs/issues/716</a>
   * @param forceCommitAddresses collection of addresses for special handling
   * @return fluent executor
   */
  public EVMExecutor forceCommitAddresses(final Collection<Address> forceCommitAddresses) {
    this.forceCommitAddresses = forceCommitAddresses;
    return this;
  }

  public EVMExecutor accessListWarmAddresses(final Set<Address> accessListWarmAddresses) {
    this.accessListWarmAddresses = accessListWarmAddresses;
    return this;
  }

  public EVMExecutor warmAddress(final Address... addresses) {
    this.accessListWarmAddresses.addAll(List.of(addresses));
    return this;
  }

  public EVMExecutor accessListWarmStorage(final Multimap<Address, Bytes32> accessListWarmStorage) {
    this.accessListWarmStorage = accessListWarmStorage;
    return this;
  }

  public EVMExecutor accessListWarmStorage(final Address address, final Bytes32... slots) {
    this.accessListWarmStorage.putAll(address, List.of(slots));
    return this;
  }

  public EVMExecutor messageCallProcessor(final MessageCallProcessor messageCallProcessor) {
    this.messageCallProcessor = messageCallProcessor;
    return this;
  }

  public EVMExecutor contractCallProcessor(
      final ContractCreationProcessor contractCreationProcessor) {
    this.contractCreationProcessor = contractCreationProcessor;
    return this;
  }
}
